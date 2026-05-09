package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a provider for a command condition.
 * The method should return boolean and can take CommandSender or Player as parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PConditionProvider {
    /**
     * The unique key of the condition.
     */
    String value();
}
