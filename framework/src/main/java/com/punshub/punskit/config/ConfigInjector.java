package com.punshub.punskit.config;

import com.punshub.punskit.annotation.OnConfigReload;
import com.punshub.punskit.annotation.Value;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Injector cho các trường được đánh dấu bằng @Value.
 */
@RequiredArgsConstructor
public class ConfigInjector {

    private final JavaPlugin plugin;
    private final PunsLogger logger;

    /**
     * Re-injects all beans with the latest configuration values.
     */
    public void reinjectAll(Collection<Object> beans) {
        for (Object bean : beans) {
            inject(bean);
            invokeReloadHook(bean);
        }
        logger.info("✓ Configuration hot-reloaded and re-injected into {} bean(s).", beans.size());
    }

    public void inject(Object bean) {
        FileConfiguration config = plugin.getConfig();
        Class<?> clazz = bean.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            Value annotation = field.getAnnotation(Value.class);
            if (annotation == null) continue;

            String path = parsePath(annotation.value());
            Object value = config.get(path);

            if (value == null) {
                if (!annotation.defaultValue().isEmpty()) {
                    value = convert(path, annotation.defaultValue(), field.getType());
                } else {
                    throw new RuntimeException("Required config path not found: " + path + 
                            " for field " + field.getName() + " in " + clazz.getSimpleName());
                }
            } else {
                value = convert(path, value, field.getType());
            }

            try {
                field.setAccessible(true);
                field.set(bean, value);
                logger.debug("Injected @Value({}) into field {} of {}", 
                        path, field.getName(), clazz.getSimpleName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject @Value into field: " + field.getName(), e);
            }
        }
    }

    private void invokeReloadHook(Object bean) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(OnConfigReload.class)) {
                try {
                    method.setAccessible(true);
                    method.invoke(bean);
                } catch (Exception e) {
                    logger.error("Failed to invoke @OnConfigReload hook in " + bean.getClass().getSimpleName(), e);
                }
            }
        }
    }

    private String parsePath(String value) {
        if (value.startsWith("${") && value.endsWith("}")) {
            return value.substring(2, value.length() - 1);
        }
        return value;
    }

    private Object convert(String path, Object value, Class<?> type) {
        if (type.isInstance(value)) return value;

        String str = String.valueOf(value);
        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(str);
            if (type == double.class || type == Double.class) return Double.parseDouble(str);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(str);
            if (type == long.class || type == Long.class) return Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to convert config value at '" + path + "' (value: " + str + ") to " + type.getSimpleName());
        }
        
        if (type == String.class) return str;
        
        // Hỗ trợ List<String> cơ bản nếu Bukkit trả về List
        if (type == List.class && value instanceof List) return value;

        return value;
    }
}
