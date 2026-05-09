# PunsKit

IoC/DI Framework nhẹ cho Minecraft Plugin — lấy cảm hứng từ Spring Boot,
được thiết kế đặc biệt cho môi trường Bukkit/PaperMC.

## Cấu trúc Module

```
PunsKit/
├── framework/          ← Core IoC Container (G1)
│   └── src/main/java/com/punshub/punskit/
│       ├── FrameworkLauncher.java       ← Entry point
│       ├── annotation/                  ← @PService, @PComponent, @PAutowired...
│       ├── container/BeanRegistry.java  ← Dependency resolver
│       ├── exception/                   ← Lỗi có message rõ ràng
│       ├── lifecycle/LifecycleManager   ← @PPostConstruct, @PPreDestroy
│       └── scanner/ClasspathScanner    ← Quét JAR tìm Bean
│
└── test-plugin/        ← Plugin mẫu để verify framework
    └── src/main/java/com/yourname/testplugin/
        ├── TestPlugin.java              ← 3 dòng để bật framework
        └── service/                    ← DatabaseService → PlayerService → GameManager
```

## Cách dùng nhanh

**1. Trong plugin của bạn:**
```java
public class MyPlugin extends JavaPlugin {
    private FrameworkLauncher framework;

    @Override
    public void onEnable() {
        framework = FrameworkLauncher.start(this, "com.myplugin");
    }

    @Override
    public void onDisable() {
        framework.shutdown();
    }
}
```

**2. Tạo Bean:**
```java
@PService
public class PlayerService {
    private final DatabaseService db;

    public PlayerService(DatabaseService db) {
        this.db = db; // framework tự inject
    }

    @PPostConstruct
    private void init() {
        // chạy sau khi inject xong
    }

    @PPreDestroy
    private void cleanup() {
        // chạy khi plugin tắt
    }
}
```

## Build

```bash
./gradlew :test-plugin:shadowJar
```

JAR output: `test-plugin/build/libs/test-plugin-1.0.0-SNAPSHOT.jar`

## Giai đoạn Phát triển

- [x] **G1** — DI Container cơ bản (Constructor Injection, Lifecycle)
- [ ] **G2** — Tích hợp Bukkit (Listener, Command, @PValue, @PScheduled)
- [ ] **G3** — ClassGraph, Brigadier, Config hot-reload
- [ ] **G4** — MethodHandles, APT compile-time (optional)
