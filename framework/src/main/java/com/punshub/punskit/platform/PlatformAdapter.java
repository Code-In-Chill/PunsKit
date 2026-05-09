package com.punshub.punskit.platform;

import com.punshub.punskit.annotation.command.PCommand;
import java.util.Collection;

/**
 * Interface trừu tượng hóa các hành vi đặc thù của từng nền tảng (Paper, Spigot, Folia).
 */
public interface PlatformAdapter {

    /**
     * Trả về loại nền tảng hiện tại.
     */
    PlatformType getType();

    /**
     * Đăng ký các bean lệnh vào hệ thống của nền tảng.
     * QUAN TRỌNG: Nên được gọi trong onLoad() để hỗ trợ tốt nhất cho Brigadier.
     *
     * @param beans Danh sách các bean đã được khởi tạo chứa annotation @PCommand.
     */
    void registerCommands(Collection<Object> beans);

    /**
     * Hủy đăng ký các lệnh khi plugin tắt.
     */
    void unregisterCommands();

    /**
     * Kiểm tra xem nền tảng có hỗ trợ Brigadier hay không.
     */
    boolean isBrigadierSupported();
}
