package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu một class là Bean được IoC Container quản lý.
 * Framework sẽ tự động khởi tạo, inject phụ thuộc và quản lý vòng đời.
 *
 * <p>Ví dụ:</p>
 * <pre>
 * {@code
 * @Service
 * public class PlayerService {
 *     private final DatabaseService db;
 *
 *     public PlayerService(DatabaseService db) {
 *         this.db = db;
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
}
