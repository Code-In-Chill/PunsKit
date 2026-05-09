# 🗺️ Kế hoạch Triển khai Multi-version Support (Java 21+) - PunsKit

Bản kế hoạch này tập trung vào việc hiện đại hóa PunsKit để hỗ trợ đa phiên bản Minecraft, ưu tiên các bản sử dụng Java 21+ để tận dụng tối đa hiệu năng và các tính năng mới.

## 1. Phạm vi hỗ trợ (Target Scope)
*   **Java Baseline:** Java 21 (Bắt buộc).
*   **Minecraft Versions:** 1.20.5, 1.20.6, 1.21, 1.21.x và các bản tương lai (1.22+).
*   **Nền tảng (Platforms):**
    *   **PaperMC:** Nền tảng ưu tiên (Sử dụng Brigadier & Lifecycle API).
    *   **Spigot/Folia:** Hỗ trợ thông qua cơ chế Fallback (CommandMap).

## 2. Kiến trúc Abstraction Layer (Adapter Pattern)

Để tránh lỗi `ClassNotFound` khi chạy trên các nền tảng khác nhau, chúng ta sẽ tách biệt logic xử lý platform.

### 2.1 Cấu trúc Module Đề xuất
```text
PunsKit/
├── framework/
│   ├── src/main/java/.../core/        (IoC, DI, Scanner - Logic thuần Java)
│   ├── src/main/java/.../platform/    (Lớp trừu tượng cho Platform)
│   │   ├── PlatformAdapter.java       (Interface định nghĩa các hành vi platform)
│   │   ├── PlatformDetector.java      (Logic nhận diện Server: Paper, Spigot, Folia)
│   │   ├── PaperAdapter.java          (Dành cho Paper 1.20.6+ - Brigadier)
│   │   └── LegacyAdapter.java         (Dành cho Spigot/Legacy - CommandMap)
```

### 2.2 Cơ chế nhận diện (Platform Detection)
Framework sẽ tự động quét sự hiện diện của các class đặc trưng theo thứ tự từ cụ thể nhất đến chung nhất:
1.  **Folia:** Check `io.papermc.paper.threadedregions.RegionizedServer`. (Kiểm tra trước vì Folia chứa cả class của Paper).
2.  **Paper Modern:** Check `io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager`.
3.  **Spigot/Legacy:** Fallback cuối cùng.

## 3. Các thành phần chính cần Refactor

### 3.1 Hệ thống Lệnh (Command System)
Đây là phần quan trọng nhất vì Paper 1.20.6 đã thay đổi hoàn toàn cách đăng ký lệnh.
*   **Interface:** `PlatformAdapter#registerCommand(...)`.
*   **Paper Implementation:** Tận dụng `LifecycleEvents.COMMANDS`.
*   **Spigot Implementation:** Sử dụng Reflection để truy cập `SimpleCommandMap`.

### 3.2 Hệ thống Logging
*   Tự động chuyển đổi giữa `SLF4J` (Paper) và `java.util.logging.Logger` (Spigot) để đảm bảo không gây lỗi khi khởi động.

### 3.3 Tích hợp XSeries (Shaded)
*   Nhúng **XSeries** để xử lý Material, Sound, và Particle đa phiên bản.
*   Developer có thể dùng `XMaterial.PLAYER_HEAD` thay vì lo lắng về sự khác biệt giữa các version.

## 4. Lộ trình Triển khai (Roadmap)

### Giai đoạn 1: Xây dựng nền tảng (Abstraction)
- [ ] Tạo Interface `PlatformAdapter`.
- [ ] Triển khai `PlatformDetector` để xác định môi trường runtime.
- [ ] Tách `BrigadierIntegration` hiện tại vào `PaperAdapter`.

### Giai đoạn 2: Refactor CommandManager
- [ ] Thay đổi `CommandManager` để sử dụng `PlatformAdapter`.
- [ ] Xây dựng `LegacyAdapter` (Fallback) cho Spigot/Bản cũ.
- [ ] Kiểm tra khả năng Tab-completion trên cả 2 nền tảng.

### Giai đoạn 3: Tối ưu hóa Java 21
- [ ] Sử dụng **Virtual Threads** (Project Loom) cho tất cả các bean được đánh dấu `@PAsync`.
- [ ] Tối ưu hóa `BeanRegistry` bằng **Pattern Matching** để code sạch hơn.

## 5. Lợi ích mang lại
1.  **Code Sạch hơn:** Không còn các đoạn `if-else` kiểm tra version rải rác.
2.  **Hiệu năng cao:** Tận dụng tối đa sức mạnh của Java 21 và Paper API mới nhất.
3.  **Dễ mở rộng:** Khi Minecraft có bản update mới, chỉ cần cập nhật Adapter tương ứng.

---
*Ngày lập kế hoạch: 09/05/2026*
*Người lập: PunsKit AI Assistant*
