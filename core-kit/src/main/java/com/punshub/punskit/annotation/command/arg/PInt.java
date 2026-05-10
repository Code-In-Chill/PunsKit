package com.punshub.punskit.annotation.command.arg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PInt {
    String name() default "number";
    int min() default Integer.MIN_VALUE;
    int max() default Integer.MAX_VALUE;
    boolean optional() default false;
    int defaultValue() default 0;
}
