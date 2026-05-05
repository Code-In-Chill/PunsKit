package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu phương thức sẽ được gọi tự động khi plugin tắt ({@code onDisable()}).
 *
 * <p>Dùng để: đóng kết nối database, lưu dữ liệu người chơi, cancel task...</p>
 *
 * <p><b>Lưu ý:</b> Framework gọi {@code @PreDestroy} theo thứ tự ngược với
 * thứ tự khởi tạo (Bean nào được tạo sau, bị hủy trước).</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreDestroy {
}
