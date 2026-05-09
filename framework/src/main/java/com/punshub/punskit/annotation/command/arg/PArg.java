package com.punshub.punskit.annotation.command.arg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generic command argument annotation.
 * Framework will infer the type from the parameter type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PArg {
    /**
     * The name of the argument for help/usage display.
     */
    String name() default "arg";

    /**
     * Optional description of the argument.
     */
    String description() default "";

    /**
     * Whether the argument is optional.
     */
    boolean optional() default false;

    /**
     * The default value if the argument is optional and not provided.
     */
    String defaultValue() default "";
}
