package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Phân biệt khi có nhiều implementation của cùng một interface.
 *
 * <p>Ví dụ: có 2 class implement {@code StorageService}: {@code MySqlStorage} và
 * {@code FileStorage}. Dùng {@code @Qualifier("mysql")} trên tham số constructor
 * để chọn đúng implementation cần inject.</p>
 *
 * <pre>
 * {@code
 * @Service
 * @Qualifier("mysql")
 * public class MySqlStorage implements StorageService { ... }
 *
 * @Service
 * public class PlayerManager {
 *     public PlayerManager(@Qualifier("mysql") StorageService storage) { ... }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface Qualifier {
    String value();
}
