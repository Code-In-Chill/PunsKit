package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ định constructor nào sẽ được dùng để inject phụ thuộc.
 *
 * <p><b>Quy tắc:</b></p>
 * <ul>
 *   <li>Nếu class chỉ có 1 constructor → không cần {@code @Autowired}, framework tự dùng.</li>
 *   <li>Nếu class có nhiều constructor → phải đánh dấu đúng 1 cái bằng {@code @Autowired}.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface Autowired {
}
