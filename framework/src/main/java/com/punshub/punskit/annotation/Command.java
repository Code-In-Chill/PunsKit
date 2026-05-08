package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a command holder.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    /**
     * The name of the command.
     */
    String name();

    /**
     * The description of the command.
     */
    String description() default "";

    /**
     * The permission required to use the command.
     */
    String permission() default "";

    /**
     * Aliases for the command.
     */
    String[] aliases() default {};
}
