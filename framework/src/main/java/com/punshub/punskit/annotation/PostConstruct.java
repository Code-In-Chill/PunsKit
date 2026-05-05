package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu phương thức sẽ được gọi tự động sau khi Bean được khởi tạo
 * và tất cả phụ thuộc đã được inject xong.
 *
 * <p><b>Quy tắc:</b> Chỉ được có đúng 1 phương thức {@code @PostConstruct} mỗi class.
 * Phương thức phải không có tham số và không có giá trị trả về (void).</p>
 *
 * <p>Dùng để: kết nối database, load file config, khởi tạo cache...</p>
 *
 * <pre>
 * {@code
 * @Service
 * public class DatabaseService {
 *     @PostConstruct
 *     private void connect() {
 *         // chạy sau khi DI xong, thay vì nhét vào constructor
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostConstruct {
}
