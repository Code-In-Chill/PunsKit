# IoC Framework for Minecraft Plugin — Kế hoạch Dự án Hoàn chỉnh

> **Phân loại:** Personal Project | **Nền tảng mục tiêu:** PaperMC 1.20.6+  
> **Ngôn ngữ:** Java 21 | **Build tool:** Gradle (Shadow Plugin)  
> **Mục tiêu cốt lõi:** Framework DI nhẹ, nhúng được vào plugin, không phụ thuộc server runtime

---

## Đánh giá & Tinh chỉnh Lộ trình Gốc

### Những điểm được giữ nguyên
- Thứ tự 4 giai đoạn (MVP → Integration → Optimization → Enterprise) — hợp lý
- Constructor Injection ưu tiên hơn Field Injection — đúng hướng
- Shading + Relocation từ ngày đầu — bắt buộc, không thể bỏ
- `onEnable()` / `onDisable()` là điểm neo vòng đời — không có cách nào khác

### Những điểm được tinh chỉnh
| Vấn đề trong lộ trình gốc | Điều chỉnh |
|---|---|
| Classpath scanning dùng `Class.forName()` thủ công ở G1 sẽ gây `ClassNotFoundException` trong môi trường PluginClassLoader | Dùng `URLClassLoader` pattern đọc JAR entries trực tiếp, không forward sang parent |
| `@Autowired` trên constructor là thừa nếu chỉ có 1 constructor | Chỉ cần `@Autowired` khi có nhiều constructor, giảm boilerplate |
| Xử lý Circular Dependency chỉ "fail-fast" chưa đủ thông tin debug | Thêm dependency path vào error message |
| G2 thiếu scope quản lý Bean (`Singleton` vs `Prototype`) | Thêm `@Scope` annotation ngay G2 |
| G3 đề xuất MethodHandles cho `@Value` injection — không cần thiết ở G3 | Đẩy xuống G4, tập trung Brigadier ở G3 |
| APT ở G4 mô tả quá sơ lược | Bổ sung chi tiết kỹ thuật và điều kiện kích hoạt |

---

## Điều kiện Tiên quyết

Trước khi bắt đầu, đảm bảo nắm vững:
- Java Reflection API (`getDeclaredConstructors`, `getDeclaredFields`, `setAccessible`)
- Cấu trúc file `.jar` (ZIP format, đọc entries)
- Bukkit plugin lifecycle (`onLoad`, `onEnable`, `onDisable`)
- Gradle Shadow Plugin cơ bản

**Thời gian ước tính toàn bộ:** 8–12 tuần làm việc bán thời gian (2–3 giờ/ngày)

---

## Giai đoạn 1 — Nền tảng DI Container (ĐÃ HOÀN THÀNH)

> **Tuần 1–2 | Kết quả:** Plugin dùng được framework để tự động khởi tạo và inject Bean

### 1.1 Thiết lập Dự án & Shading

**Cấu trúc thư mục:**
```
PunsKit/
├── framework/          ← module framework core
│   ├── src/main/java/com/punshub/punskit/
│   └── build.gradle
├── test-plugin/        ← plugin mẫu để test
│   ├── src/main/java/
│   └── build.gradle
└── settings.gradle
```

**`build.gradle` của framework module:**
```groovy
plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT'
}

shadowJar {
    relocate 'com.punshub.punskit', "com.yourname.${project.name}.shaded.PunsKit"
    // Relocation bắt buộc — mỗi plugin sẽ có bản riêng của framework
}
```

**Checklist:**
- [x] Multi-module Gradle setup chạy được
- [x] `shadowJar` task tạo ra JAR có package đã được relocate
- [x] test-plugin depend vào framework và build thành công

---

### 1.2 Hệ thống Annotation Cốt lõi

Tạo các annotation với `@Retention(RetentionPolicy.RUNTIME)`:

```
com.punshub.punskit.annotation/
├── Component.java      ← alias của Service, dùng cho non-service classes  
├── Service.java        ← đánh dấu Bean chính
├── Autowired.java      ← chỉ cần khi có nhiều constructor (tinh chỉnh so với gốc)
├── Qualifier.java      ← phân biệt khi có nhiều impl của 1 interface
├── Primary.java        ← ưu tiên khi có nhiều impl, không cần @Qualifier
├── PostConstruct.java  ← chạy sau khi DI xong
└── PreDestroy.java     ← chạy khi plugin tắt
```

**Quy tắc thiết kế:**
- Nếu class chỉ có **1 constructor** → tự động dùng constructor đó, không cần `@Autowired`
- Nếu có **nhiều constructor** → bắt buộc đánh dấu `@Autowired` đúng 1 cái
- `@PostConstruct` chỉ được có **1 phương thức** mỗi class, ném `FrameworkException` nếu vi phạm

**Checklist:**
- [x] Tất cả annotation được tạo với đúng `@Retention` và `@Target`
- [x] Viết Javadoc cho từng annotation mô tả hành vi

---

### 1.3 Classpath Scanner (Giai đoạn MVP)

> **Lưu ý tinh chỉnh:** Không dùng `Class.forName()` với tên package vì sẽ kích hoạt parent ClassLoader chain, gây lỗi trong môi trường Bukkit.

**Cách đúng — đọc JAR entries trực tiếp:**

```java
public class ClasspathScanner {
    
    public Set<Class<?>> scan(JavaPlugin plugin, String basePackage) {
        Set<Class<?>> candidates = new HashSet<>();
        ClassLoader classLoader = plugin.getClass().getClassLoader();
        // classLoader ở đây chính là PluginClassLoader
        
        // Lấy đường dẫn đến file .jar của plugin
        URL jarUrl = plugin.getClass().getProtectionDomain()
                         .getCodeSource().getLocation();
        
        String packagePath = basePackage.replace('.', '/');
        
        try (JarFile jar = new JarFile(new File(jarUrl.toURI()))) {
            jar.stream()
               .filter(e -> e.getName().startsWith(packagePath) 
                         && e.getName().endsWith(".class"))
               .forEach(entry -> {
                   String className = entry.getName()
                       .replace('/', '.').replace(".class", "");
                   try {
                       // Dùng đúng PluginClassLoader để load
                       Class<?> clazz = classLoader.loadClass(className);
                       if (isCandidate(clazz)) {
                           candidates.add(clazz);
                       }
                   } catch (ClassNotFoundException ignored) {}
               });
        }
        return candidates;
    }
    
    private boolean isCandidate(Class<?> clazz) {
        return clazz.isAnnotationPresent(Service.class) 
            || clazz.isAnnotationPresent(Component.class);
    }
}
```

**Checklist:**
- [x] Scanner tìm đúng class trong JAR của plugin
- [x] Scanner không quét sang class của Bukkit/Paper
- [x] Scanner bỏ qua interfaces và abstract classes

---

### 1.4 Dependency Resolver & Bean Registry

**Cấu trúc Registry:**
```java
public class BeanRegistry {
    // Map chính: Class → Instance
    private final Map<Class<?>, Object> beans = new LinkedHashMap<>();
    
    // Map phụ: Interface → Implementation (cho injection qua interface)
    private final Map<Class<?>, List<Class<?>>> interfaceMap = new HashMap<>();
    
    // Tracking để phát hiện circular dependency
    private final Set<Class<?>> currentlyResolving = new LinkedHashSet<>();
}
```

**Thuật toán giải quyết phụ thuộc (tinh chỉnh error message):**

```
FUNCTION resolve(Class target):
    IF target IN beans → return beans[target]
    
    IF target IN currentlyResolving:
        // Tinh chỉnh: hiển thị toàn bộ dependency path
        path = currentlyResolving.join(" → ") + " → " + target
        THROW CircularDependencyException("Circular dependency: " + path)
    
    ADD target TO currentlyResolving
    
    constructor = findConstructor(target)  // @Autowired hoặc duy nhất
    params = constructor.getParameterTypes()
    
    args = []
    FOR EACH param IN params:
        resolved = resolveByType(param)  // xử lý @Qualifier/@Primary ở đây
        args.ADD(resolved)
    
    instance = constructor.newInstance(args)
    beans.PUT(target, instance)
    REMOVE target FROM currentlyResolving
    
    return instance
```

**Xử lý nhiều implementation (tinh chỉnh so với gốc):**
1. Nếu có đúng 1 impl → dùng luôn
2. Nếu có nhiều impl:
   - Tìm impl có `@Primary` → dùng
   - Tìm theo `@Qualifier("name")` → dùng
   - Nếu không xác định được → ném `AmbiguousBeanException` với danh sách impl

**Checklist:**
- [x] Khởi tạo thành công dependency graph đơn giản (A → B → C)
- [x] Phát hiện và báo lỗi circular dependency với path đầy đủ
- [x] Xử lý đúng khi inject qua interface
- [x] `@Primary` và `@Qualifier` hoạt động

---

### 1.5 PunskitPlugin & JavaPlugin Integration (ĐÃ HOÀN THÀNH)

Để tối ưu hóa trải nghiệm (DX), framework cung cấp class abstract `PunskitPlugin` kế thừa `JavaPlugin`. Người dùng chỉ cần kế thừa class này để tự động kích hoạt IoC Container mà không cần viết code khởi tạo thủ công.

```java
public abstract class PunskitPlugin extends JavaPlugin {
    private FrameworkLauncher launcher;
    
    @Override
    public final void onEnable() {
        String basePkg = detectBasePackage(); // Tự động lấy package của class con
        this.launcher = FrameworkLauncher.start(this, basePkg);
        onPluginEnable();
    }
    
    @Override
    public final void onDisable() {
        onPluginDisable();
        if (launcher != null) launcher.shutdown();
    }
    
    public void onPluginEnable() {}
    public void onPluginDisable() {}
    public <T> T getBean(Class<T> type) { return launcher.getBean(type); }
}
```

`FrameworkLauncher` vẫn được giữ lại như một "Engine" xử lý logic bên dưới (Internal Orchestrator), trong khi `PunskitPlugin` đóng vai trò là giao diện người dùng (User-facing API).

**Checklist:**
- [x] `PunskitPlugin` tự động phát hiện package và quản lý lifecycle
- [x] `onPluginEnable()` và `onPluginDisable()` hoạt động như hook thay thế
- [x] `getBean()` có sẵn ngay trong class Plugin
- [x] `FrameworkLauncher` xử lý việc quét class, đăng ký bean và quản lý lifecycle nội bộ
- [x] Log rõ ràng từng bước (SLF4J Fluent API + Enterprise PunsLogger)
- [x] Sử dụng Lombok để giảm boilerplate

---

### Milestone G1 — Kiểm tra Tích hợp

Tạo plugin test với 3 class:
```
DatabaseService (không có dependency)
    ↑
PlayerService (cần DatabaseService)
    ↑
GameManager (cần PlayerService)
```

Kỳ vọng: Server khởi động → log hiện 3 Bean được tạo đúng thứ tự → `/stop` → log hiện 3 Bean bị hủy ngược thứ tự.

- [x] **Kết quả:** Đạt (Xác minh qua các file trong test-plugin)

---

## Hạ tầng & CI/CD (ĐÃ HOÀN THÀNH)

> **Kết quả:** Quy trình CI/CD tự động, nghiêm ngặt và chuyên nghiệp trên GitHub.

### 1.1 Luồng Nhánh (Branching Strategy)
- [x] Thiết lập luồng: `feature/*` -PR-> `develop` -PR-> `master`.
- [x] **Branch Flow Enforcer:** Chặn các PR không đúng quy trình (ví dụ: `feature` trực tiếp vào `master`).

### 1.2 Kiểm tra Tự động (CI)
- [x] **Quickcheck:** Tự động build (`assemble`) cho mọi PR vào `develop` và `master`.
- [x] Phải vượt qua Quickcheck mới cho phép merge.

### 1.3 Phát hành & Đóng gói (CD)
- [x] **Auto-versioning:** Tự động xác định version (patch, minor, major) dựa trên keyword trong commit message.
- [x] **GitHub Release:** Tự động tạo tag và release notes khi merge vào `master`.
- [x] **Public Package:** Tự động publish framework lên GitHub Packages.

---

## Giai đoạn 2 — Tích hợp Hệ sinh thái Bukkit/Paper (ĐANG THỰC HIỆN)

> **Tuần 3–4 | Kết quả:** Framework tự động xử lý Events, Commands, Config, Schedulers

### 2.1 Bean Scope (Tinh chỉnh — thêm mới so với gốc)

Trước khi tích hợp Bukkit API, cần thêm khái niệm scope vì một số thứ như listener cần singleton, nhưng factory class cần prototype.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scope {
    ScopeType value() default ScopeType.SINGLETON;
}

public enum ScopeType {
    SINGLETON,   // mặc định — 1 instance duy nhất
    PROTOTYPE    // tạo mới mỗi lần được inject
}
```

**Checklist:**
- [ ] `SINGLETON` hoạt động đúng (cùng instance được inject mọi nơi)
- [ ] `PROTOTYPE` tạo instance mới mỗi lần

---

### 2.2 Tự động Đăng ký Event Listener

**Logic trong BeanRegistry sau khi tạo Bean:**
```java
private void postProcess(Object bean) {
    if (bean instanceof Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        logger.info("Auto-registered listener: " + bean.getClass().getSimpleName());
    }
}
```

Không cần annotation tùy chỉnh — implement `Listener` là đủ tín hiệu.

**Checklist:**
- [ ] Class implements Listener + @Service → tự động đăng ký
- [ ] Khi `shutdown()`, unregister tất cả listener đã đăng ký (tránh leak khi reload)
- [ ] Test: PlayerJoinEvent được nhận mà không cần gọi `registerEvents()` thủ công

---

### 2.3 Đăng ký Lệnh Động

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    String name();
    String description() default "";
    String permission() default "";
    String[] aliases() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandHandler {
    // đánh dấu method xử lý lệnh chính
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subcommand {
    String value();  // ví dụ: "give <player> <amount>"
}
```

**Cơ chế đăng ký vào CommandMap:**
```java
private void registerCommand(Object bean, Command cmdAnnotation) {
    try {
        Field cmdMapField = Bukkit.getServer().getClass()
            .getDeclaredField("commandMap");
        cmdMapField.setAccessible(true);
        CommandMap commandMap = (CommandMap) cmdMapField.get(Bukkit.getServer());
        
        PluginCommand command = createPluginCommand(cmdAnnotation.name(), plugin);
        command.setExecutor((sender, cmd, label, args) -> {
            // routing đến đúng method trong bean
            return routeCommand(bean, args);
        });
        
        commandMap.register(plugin.getName().toLowerCase(), command);
    } catch (Exception e) {
        logger.severe("Failed to register command: " + cmdAnnotation.name());
    }
}
```

**Checklist:**
- [x] Lệnh hoạt động mà không cần khai báo trong `plugin.yml`
- [x] `@Subcommand` route đúng đến method tương ứng
- [x] Permission check được áp dụng tự động
- [x] Tab completion cơ bản hoạt động (Hoàn thiện ở G3)

---

### 2.4 Config Injection (@Value)

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Value {
    String value();  // ví dụ: "${database.host}"
    String defaultValue() default "";
}
```

**Cơ chế injection sau khi Bean được tạo:**
```java
private void injectConfigValues(Object bean) {
    FileConfiguration config = plugin.getConfig();
    
    for (Field field : bean.getClass().getDeclaredFields()) {
        Value valueAnnotation = field.getAnnotation(Value.class);
        if (valueAnnotation == null) continue;
        
        String path = parseConfigPath(valueAnnotation.value()); // bỏ ${}
        field.setAccessible(true);
        
        Object value = config.get(path);
        if (value == null) value = parseDefault(valueAnnotation.defaultValue(), field.getType());
        if (value == null) throw new ConfigValueNotFoundException(path, bean.getClass());
        
        field.set(bean, convertType(value, field.getType()));
    }
}
```

**Type converter cần hỗ trợ:** `String`, `int/Integer`, `double/Double`, `boolean/Boolean`, `List<String>`

**Checklist:**
- [ ] `@Value("${server.name}")` inject đúng giá trị String
- [ ] Type conversion hoạt động cho các kiểu cơ bản
- [ ] `defaultValue` được dùng khi key không tồn tại trong config
- [ ] Ném lỗi rõ ràng khi key bắt buộc không tồn tại

---

### 2.5 Scheduler Annotation

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scheduled {
    long delay() default 0;      // ticks trước khi chạy lần đầu
    long period() default 20;    // ticks giữa mỗi lần chạy (20 = 1 giây)
    boolean async() default false;
    boolean runOnce() default false;  // chạy 1 lần thì dừng
}
```

```java
private void registerSchedulers(Object bean) {
    for (Method method : bean.getClass().getDeclaredMethods()) {
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        if (scheduled == null) continue;
        
        Runnable task = () -> {
            try { method.invoke(bean); }
            catch (Exception e) { logger.warning("Scheduler error in " + method.getName()); }
        };
        
        BukkitScheduler scheduler = Bukkit.getScheduler();
        if (scheduled.runOnce()) {
            if (scheduled.async()) scheduler.runTaskLaterAsynchronously(plugin, task, scheduled.delay());
            else scheduler.runTaskLater(plugin, task, scheduled.delay());
        } else {
            if (scheduled.async()) scheduler.runTaskTimerAsynchronously(plugin, task, scheduled.delay(), scheduled.period());
            else scheduler.runTaskTimer(plugin, task, scheduled.delay(), scheduled.period());
        }
    }
}
```

**Checklist:**
- [ ] Method được gọi đúng interval
- [ ] `async = true` không chạy trên main thread (verify bằng `Bukkit.isPrimaryThread()`)
- [ ] Task được cancel khi plugin tắt (lưu BukkitTask reference, cancel trong shutdown)

---

### Milestone G2 — Kiểm tra Tích hợp

Plugin test thực tế với:
- `@Service PlayerListener implements Listener` → tự nhận PlayerJoinEvent
- `@Service @Command(name="heal")` → lệnh `/heal` hoạt động không cần `plugin.yml`
- `@Value("${messages.welcome}")` → inject chuỗi từ config.yml
- `@Scheduled(period = 200, async = true)` → task chạy mỗi 10 giây trên async thread

---

## Giai đoạn 3 — Tối ưu hóa & PaperMC Hiện đại

> **Tuần 5–6 | Kết quả:** Hiệu năng tốt hơn, tương thích Brigadier, production-ready

### 3.1 Nâng cấp Scanner lên ClassGraph

**Thêm dependency:**
```groovy
implementation 'io.github.classgraph:classgraph:4.8.174'
```

**Cách dùng đúng trong môi trường Bukkit:**
```java
public Set<Class<?>> scan(JavaPlugin plugin, String basePackage) {
    ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();
    
    try (ScanResult result = new ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .ignoreParentModuleLayers()        // không quét module của JVM
            .overrideClassLoaders(pluginClassLoader)  // chỉ quét ClassLoader của plugin
            .acceptPackages(basePackage)
            .scan()) {
        
        return result.getClassesWithAnnotation(Service.class.getName())
            .union(result.getClassesWithAnnotation(Component.class.getName()))
            .stream()
            .filter(ci -> !ci.isInterface() && !ci.isAbstract())
            .map(ci -> {
                try { return pluginClassLoader.loadClass(ci.getName()); }
                catch (ClassNotFoundException e) { return null; }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
```

**Lưu ý quan trọng:** Phải `shade` ClassGraph vào JAR và relocate package của nó. Không để ClassGraph là optional dependency.

**Checklist:**
- [ ] Scan nhanh hơn so với cách thủ công G1 (đo bằng System.nanoTime())
- [ ] Không quét class của Paper/Bukkit (verify bằng log)
- [ ] Hoạt động ổn định khi plugin có 50+ class

---

### 3.2 Brigadier Integration (PaperMC 1.20.6+)

**Thay thế CommandMap bằng LifecycleEventManager:**

```java
// Trong FrameworkLauncher, gọi trong onLoad() của plugin (không phải onEnable)
public void registerBrigadierCommands(LifecycleEventManager<Plugin> manager) {
    manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
        Commands commands = event.registrar();
        
        for (Object bean : registry.getBeansWithAnnotation(Command.class)) {
            Command cmdAnnotation = bean.getClass().getAnnotation(Command.class);
            LiteralCommandNode<CommandSourceStack> node = buildCommandTree(bean, cmdAnnotation);
            commands.register(node, cmdAnnotation.description());
        }
    });
}

private LiteralCommandNode<CommandSourceStack> buildCommandTree(Object bean, Command annotation) {
    var root = net.minecraft.commands.Commands.literal(annotation.name());
    
    // Quét các @Subcommand method và build tree
    for (Method method : bean.getClass().getDeclaredMethods()) {
        Subcommand sub = method.getAnnotation(Subcommand.class);
        if (sub != null) {
            root.then(buildSubcommandNode(bean, method, sub));
        }
    }
    
    return root.build();
}
```

**Lưu ý:** Brigadier chỉ available trên PaperMC, không có trên Spigot thuần. Cần guard:
```java
private boolean isBrigadierAvailable() {
    try {
        Class.forName("io.papermc.paper.command.brigadier.Commands");
        return true;
    } catch (ClassNotFoundException e) {
        return false;
    }
}
```

**Checklist:**
- [ ] Lệnh đăng ký qua Brigadier hiện tab-completion client-side
- [ ] Lệnh vẫn hoạt động sau `/reload`
- [ ] Fallback về CommandMap nếu không phải PaperMC

---

### 3.3 Config Hot-reload (Bonus G3)

Khi admin chạy lệnh reload config, tự động re-inject `@Value` vào tất cả Bean:

```java
public void reloadConfig() {
    plugin.reloadConfig();
    registry.getAllBeans().forEach(bean -> {
        injectConfigValues(bean);  // re-inject từ config mới
        invokePostConstruct(bean); // gọi lại @PostConstruct nếu cần
    });
}
```

Tạo built-in command `/[pluginname] reload` tự động có trong mọi plugin dùng framework.

**Checklist:**
- [ ] Config thay đổi → Bean nhận giá trị mới mà không cần restart server
- [ ] `@PostConstruct` được gọi lại sau reload nếu được đánh dấu `reinvokeOnReload = true`

---

### Milestone G3

- Đo thời gian startup của framework với 20 Bean: phải < 50ms
- Test `/reload` trên PaperMC: lệnh Brigadier vẫn hoạt động
- Kiểm tra không có memory leak bằng cách watch Metaspace sau nhiều lần reload

---

## Giai đoạn 4 — Kiến trúc Enterprise (Tùy chọn, Dài hạn)

> **Tuần 7–12 | Kết quả:** Zero-reflection runtime, compile-time validation

### 4.1 MethodHandles Optimization

Áp dụng khi có benchmark thực tế chứng minh Reflection là bottleneck. Không làm sớm.

```java
// Thay Field.set():
MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup());
MethodHandle setter = lookup.findSetter(targetClass, fieldName, fieldType);
setter.invoke(beanInstance, value);

// Lưu MethodHandle dưới dạng static final để JIT inline:
private static final MethodHandle DB_URL_SETTER = initSetter();
```

**Khi nào mới làm:** Khi plugin có 100+ Bean và đo được startup time > 200ms.

---

### 4.2 Annotation Processor (APT) — Compile-time DI

Đây là tính năng phức tạp nhất, chỉ làm khi G1-G3 hoàn chỉnh và ổn định.

**Cơ chế hoạt động:**
```
Compile time:
  @Service DatabaseService → APT đọc → sinh ra DatabaseServiceFactory.java
  
Runtime:
  Framework chỉ gọi new DatabaseServiceFactory().create()
  Không có Reflection, không có ClassGraph scan
```

**Tạo AbstractProcessor:**
```java
@SupportedAnnotationTypes({"com.punshub.punskit.annotation.Service"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class MiocAnnotationProcessor extends AbstractProcessor {
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(Service.class)) {
            TypeElement typeElement = (TypeElement) element;
            generateFactory(typeElement);
        }
        return true;
    }
    
    private void generateFactory(TypeElement serviceClass) {
        // Dùng JavaPoet để sinh mã nguồn factory
        // Output: ${ServiceName}Factory.java với hardcoded constructor calls
    }
}
```

**Dependency cần thêm:** `com.squareup:javapoet:1.13.0` để sinh mã Java dễ dàng hơn.

**Checklist:**
- [ ] APT sinh ra factory class khi biên dịch
- [ ] Framework tự phát hiện và dùng factory class nếu tồn tại
- [ ] Fallback về Runtime Reflection nếu không có factory (backward compatible)
- [ ] Lỗi circular dependency được báo tại compile-time (thay vì runtime)

---

## Tóm tắt Timeline

```
Tuần 1  │ G1: Setup dự án, Annotations, ClasspathScanner
Tuần 2  │ G1: Dependency Resolver, FrameworkLauncher, test E2E
Tuần 3  │ G2: Listener auto-register, Dynamic Command, @Value
Tuần 4  │ G2: @Scheduled, Bean Scope, Milestone G2 test
Tuần 5  │ G3: ClassGraph migration, Brigadier integration
Tuần 6  │ G3: Config hot-reload, đo hiệu năng, Milestone G3
Tuần 7+ │ G4: MethodHandles (nếu cần), APT (nếu muốn)
```

---

## Quyết định Kỹ thuật Quan trọng

| Câu hỏi | Quyết định | Lý do |
|---|---|---|
| Maven hay Gradle? | **Gradle + Shadow** | Linh hoạt hơn cho multi-module, Shadow plugin tốt hơn maven-shade |
| Field injection hay Constructor injection? | **Constructor injection** | Immutability, testable, bắt lỗi sớm hơn |
| Spigot hay PaperMC làm target chính? | **PaperMC** | Brigadier, LifecycleEventManager chỉ có trên Paper |
| Shading là optional hay bắt buộc? | **Bắt buộc** | Không shade = ClassLoader conflict khi nhiều plugin dùng framework |
| APT từ đầu hay sau? | **Sau (G4)** | APT tăng compile time, phức tạp setup, lợi ích nhỏ ở giai đoạn đầu |

---

## Rủi ro và Cách Giảm thiểu

| Rủi ro | Khả năng xảy ra | Cách xử lý |
|---|---|---|
| Paper API thay đổi Brigadier interface | Trung bình | Abstract layer riêng cho command system, dễ swap |
| ClassGraph quét nhầm class của server | Cao nếu cấu hình sai | `overrideClassLoaders()` + `acceptPackages()` giới hạn scope |
| Circular dependency khó debug | Thấp nếu làm đúng | Hiển thị full dependency path trong exception |
| Memory leak do không unregister task/listener | Trung bình | Shutdown hook bắt buộc cancel tất cả BukkitTask |
| Scope creep — thêm quá nhiều tính năng | Cao (personal project) | Bám sát milestone, chỉ thêm tính năng sau khi milestone hiện tại pass test |

---

## Định nghĩa "Hoàn thành" cho Từng Giai đoạn

- [x] **G1 Done:** Một plugin với 5+ Bean khởi động không lỗi, DI graph đúng, shutdown sạch
- [ ] **G2 Done:** Plugin mini-game đơn giản được xây hoàn toàn bằng framework, không có `registerEvents()`/`registerCommand()` thủ công

- **G3 Done:** Startup < 50ms với 20 Bean, Brigadier tab-completion hoạt động, config reload không crash
- **G4 Done:** APT sinh factory class, runtime không dùng Reflection cho known Bean

---

*Cập nhật lần cuối: theo đánh giá và tinh chỉnh từ báo cáo nghiên cứu gốc*
ốc*
