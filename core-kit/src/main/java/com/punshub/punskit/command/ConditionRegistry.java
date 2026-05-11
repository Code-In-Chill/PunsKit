package com.punshub.punskit.command;

import com.punshub.punskit.annotation.command.PConditionProvider;
import com.punshub.punskit.exception.FrameworkException;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
        if (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class) {
            throw new FrameworkException("Condition provider method '" + method.getName() + "' in " + 
                    bean.getClass().getSimpleName() + " must return boolean.");
        }

        Parameter[] params = method.getParameters();
        if (params.length > 1) {
            throw new FrameworkException("Condition provider method '" + method.getName() + "' in " + 
                    bean.getClass().getSimpleName() + " must have 0 or 1 parameter.");
        }

        Class<?> paramType = params.length == 1 ? params[0].getType() : null;
        if (paramType != null && !CommandSender.class.isAssignableFrom(paramType) && !Player.class.isAssignableFrom(paramType)) {
            throw new FrameworkException("Condition provider parameter in '" + method.getName() + "' must be CommandSender or Player.");
        }

        method.setAccessible(true);
        conditions.put(key, (sender) -> {
            try {
                if (paramType == null) {
                    Object result = method.invoke(bean);
                    return result instanceof Boolean && (Boolean) result;
                }

                if (Player.class.isAssignableFrom(paramType)) {
                    if (!(sender instanceof Player)) return false;
                    Player player = (Player) sender;
                    Object result = method.invoke(bean, player);
                    return result instanceof Boolean && (Boolean) result;
                }

                Object result = method.invoke(bean, sender);
                return result instanceof Boolean && (Boolean) result;
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
