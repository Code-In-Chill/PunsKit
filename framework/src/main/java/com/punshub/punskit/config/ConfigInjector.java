package com.punshub.punskit.config;

import com.punshub.punskit.annotation.Value;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Injector cho các trường được đánh dấu bằng @Value.
 */
@RequiredArgsConstructor
public class ConfigInjector {

    private final JavaPlugin plugin;
    private final PunsLogger logger;

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
                    value = convert(annotation.defaultValue(), field.getType());
                } else {
                    logger.warn("Config path not found: {} for field {} in {}", 
                            path, field.getName(), clazz.getSimpleName());
                    continue;
                }
            } else {
                value = convert(value, field.getType());
            }

            try {
                field.setAccessible(true);
                field.set(bean, value);
                logger.debug("Injected @Value({}) into field {} of {}", 
                        path, field.getName(), clazz.getSimpleName());
            } catch (Exception e) {
                logger.error("Failed to inject @Value into field: " + field.getName(), e);
            }
        }
    }

    private String parsePath(String value) {
        if (value.startsWith("${") && value.endsWith("}")) {
            return value.substring(2, value.length() - 1);
        }
        return value;
    }

    private Object convert(Object value, Class<?> type) {
        if (type.isInstance(value)) return value;

        String str = String.valueOf(value);
        if (type == int.class || type == Integer.class) return Integer.parseInt(str);
        if (type == double.class || type == Double.class) return Double.parseDouble(str);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(str);
        if (type == long.class || type == Long.class) return Long.parseLong(str);
        if (type == String.class) return str;
        
        // Hỗ trợ List<String> cơ bản nếu Bukkit trả về List
        if (type == List.class && value instanceof List) return value;

        return value;
    }
}
