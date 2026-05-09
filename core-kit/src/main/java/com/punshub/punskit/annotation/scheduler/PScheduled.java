package com.punshub.punskit.annotation.scheduler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Schedules a method to be run by the Bukkit scheduler.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PScheduled {
    /**
     * Delay before the first execution (in ticks).
     */
    long delay() default 0;

    /**
     * Period between successive executions (in ticks).
     */
    long period() default -1;

    /**
     * Whether the task should run asynchronously.
     */
    boolean async() default false;

    /**
     * Whether the task should run only once.
     */
    boolean runOnce() default false;
}
