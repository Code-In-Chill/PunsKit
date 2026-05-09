package com.punshub.punskit.annotation.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a provider for a command condition.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PConditionProvider {
    /**
     * The unique key of the condition.
     */
    String value();
}
