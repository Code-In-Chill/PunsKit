package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu implementation ưu tiên khi có nhiều impl của cùng 1 interface,
 * mà không muốn dùng {@link Qualifier}.
 *
 * <p>Chỉ được có đúng 1 class {@code @Primary} cho mỗi interface.
 * Nếu có nhiều hơn, framework sẽ ném {@code AmbiguousBeanException}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Primary {
}
