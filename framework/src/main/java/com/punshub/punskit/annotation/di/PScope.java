package com.punshub.punskit.annotation.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Xác định phạm vi (scope) của Bean.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PScope {
    PScopeType value() default PScopeType.SINGLETON;
}
