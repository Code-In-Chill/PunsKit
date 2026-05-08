package com.punshub.punskit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a value from the plugin's configuration.
 * Supports placeholder format: "${path.to.value}" or simple path: "path.to.value".
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Value {
    /**
     * The path to the value in the config.yml.
     */
    String value();

    /**
     * The default value if the path is not found.
     */
    String defaultValue() default "";
}
