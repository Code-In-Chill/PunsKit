package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Schedules a method to be run by the Bukkit scheduler.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scheduled {
    /**
     * Delay before the first execution (in ticks).
     */
    long delay() default 0;

    /**
     * Period between successive executions (in ticks).
     * If 0 or less, the task runs only once.
     */
    long period() default -1;

    /**
     * Whether the task should run asynchronously.
     */
    boolean async() default false;

    /**
     * Whether the task should run only once.
     * Alias for setting period to -1.
     */
    boolean runOnce() default false;
}
