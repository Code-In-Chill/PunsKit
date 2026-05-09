package com.punshub.punskit.annotation.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PAsync {
    /**
     * Whether the framework should automatically synchronize back to the main thread
     * after the method execution is complete.
     */
    boolean syncOnComplete() default false;
}
