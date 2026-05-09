package com.punshub.punskit.command;

import com.punshub.punskit.annotation.PConditionProvider;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Quản lý các điều kiện tùy chỉnh cho lệnh.
 */
@RequiredArgsConstructor
public class ConditionRegistry {

    private final PunsLogger logger;
    private final Map<String, Predicate<CommandSender>> conditions = new HashMap<>();

    public void registerProviders(Collection<Object> beans) {
        int count = 0;
        for (Object bean : beans) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                PConditionProvider anno = method.getAnnotation(PConditionProvider.class);
                if (anno != null) {
                    registerProvider(bean, method, anno.value());
                    count++;
                }
            }
        }
        if (count > 0) {
            logger.info("✓ Registered {} custom command condition(s).", count);
        }
    }

    private void registerProvider(Object bean, Method method, String key) {
        method.setAccessible(true);
        conditions.put(key, (sender) -> {
            try {
                Object result = method.invoke(bean, sender);
                if (result instanceof Boolean b) return b;
                return true;
            } catch (Exception e) {
                logger.error("Error evaluating condition: " + key, e);
                return false;
            }
        });
        logger.debug("Registered condition provider: {}", key);
    }

    public boolean check(String key, CommandSender sender) {
        Predicate<CommandSender> condition = conditions.get(key);
        if (condition == null) {
            logger.warn("Condition not found: {}. Failing closed for security.", key);
            return false;
        }
        return condition.test(sender);
    }
}
