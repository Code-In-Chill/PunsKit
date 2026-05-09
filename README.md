# PunsKit

IoC/DI Framework nhẹ cho Minecraft Plugin — lấy cảm hứng từ Spring Boot,
được thiết kế đặc biệt cho môi trường Bukkit/PaperMC.

## Cấu trúc Module

```
PunsKit/
├── core-kit/           ← Core IoC Container & Platform Abstraction (G1-G3)
│   └── src/main/java/com/punshub/punskit/
│       ├── PunskitPlugin.java           ← Entry point (Kế thừa class này)
│       ├── FrameworkLauncher.java       ← Framework engine
│       ├── annotation/                  ← @PService, @PCommand, @PValue...
│       ├── container/BeanRegistry.java  ← Dependency resolver
│       ├── platform/                    ← Multi-version Adapter (Paper/Spigot)
│       └── scanner/ClasspathScanner    ← Quét JAR bằng ClassGraph
│
└── test-plugin/        ← Plugin mẫu để verify framework
    └── src/main/java/com/yourname/testplugin/
        ├── TestPlugin.java              ← Kế thừa PunskitPlugin
        └── service/                    ← Các Bean thực tế
```

## Cách dùng nhanh

**1. Trong plugin của bạn:**
Thay vì kế thừa `JavaPlugin`, bạn kế thừa `PunskitPlugin`. Framework sẽ tự động lo mọi thứ về vòng đời!

```java
package com.myplugin;

import com.punshub.punskit.PunskitPlugin;

public class MyPlugin extends PunskitPlugin {

    @Override
    public void onPluginEnable() {
        getLogger().info("Plugin started with PunsKit!");
    }
}
```

**2. Tạo Bean (Service, Command, Listener):**
```java
@PService
@PCommand(name = "hello")
public class HelloCommand implements org.bukkit.event.Listener {
    private final DatabaseService db;

    // Framework tự động inject dependencies
    public HelloCommand(DatabaseService db) {
        this.db = db; 
    }

    @PCommandHandler
    public void execute(@PSender Player player) {
        player.sendMessage("Hello from PunsKit!");
    }

    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        // Framework cũng tự động đăng ký event này!
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
- [x] **G2** — Tích hợp Bukkit (Listener, Command, @PValue, @PScheduled)
- [x] **G2.5** — Multi-version Abstraction (PaperMC, Folia, Spigot)
- [x] **G3** — ClassGraph, Brigadier Native, Config hot-reload, @PAsync
- [ ] **G4** — MethodHandles, APT compile-time (optional)
