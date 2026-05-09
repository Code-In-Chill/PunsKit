package com.punshub.punskit.annotation.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu phương thức sẽ được gọi tự động sau khi Bean được khởi tạo
 * và tất cả phụ thuộc đã được inject xong.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PPostConstruct {
    /**
     * Whether this method should be re-invoked when the configuration is hot-reloaded.
     * Note: If true, the framework will attempt to unregister existing listeners for this bean 
     * before re-invoking to prevent duplicates.
     */
    boolean reinvokeOnReload() default false;
}
