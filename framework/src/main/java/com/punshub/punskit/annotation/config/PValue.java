package com.punshub.punskit.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a value from the plugin's configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PValue {
    /**
     * The path to the value in the config.yml.
     */
    String value();

    /**
     * The default value if the path is not found.
     */
    String defaultValue() default "";
}
