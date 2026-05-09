package com.punshub.punskit.annotation.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires a specific condition to be met before executing the command.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PCondition {
    /**
     * The unique key of the condition.
     */
    String value();

    /**
     * Optional message to send if the condition fails.
     */
    String message() default "§cYou do not meet the requirements to use this command.";
}
