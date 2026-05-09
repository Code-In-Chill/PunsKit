package com.punshub.punskit.lifecycle;

import com.punshub.punskit.annotation.di.PPostConstruct;
import com.punshub.punskit.annotation.di.PPreDestroy;
import com.punshub.punskit.exception.FrameworkException;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Quản lý vòng đời của Bean: gọi {@code @PPostConstruct} khi khởi tạo
 * và {@code @PPreDestroy} khi plugin tắt.
 */
@RequiredArgsConstructor
public class LifecycleManager {

    private final PunsLogger logger;

    public void invokePostConstructAll(Collection<Object> beans) {
        for (Object bean : beans) {
            invokePostConstruct(bean);
        }
    }

    public void invokePostConstructOnReload(Collection<Object> beans) {
        for (Object bean : beans) {
            List<Method> methods = findAnnotatedMethods(bean.getClass(), PPostConstruct.class);
            for (Method method : methods) {
                PPostConstruct annotation = method.getAnnotation(PPostConstruct.class);
                if (annotation.reinvokeOnReload()) {
                    // Tránh duplicate listener nếu bean là Listener và @PPostConstruct thực hiện đăng ký
                    if (bean instanceof org.bukkit.event.Listener listener) {
                        org.bukkit.event.HandlerList.unregisterAll(listener);
                        logger.debug("Unregistered listener for {} before @PPostConstruct reinvocation.", 
                                bean.getClass().getSimpleName());
                    }
                    invoke(bean, method, "@PPostConstruct (Reload)");
                }
            }
        }
    }

    public void invokePreDestroyAll(Collection<Object> beans) {
        List<Object> reversed = new ArrayList<>(beans);
        Collections.reverse(reversed);

        for (Object bean : reversed) {
            invokePreDestroy(bean);
        }
    }

    private void invokePostConstruct(Object bean) {
        List<Method> methods = findAnnotatedMethods(bean.getClass(), PPostConstruct.class);

        if (methods.size() > 1) {
            throw new FrameworkException(
                    "Bean '" + bean.getClass().getSimpleName() + "' has " + methods.size() +
                    " @PPostConstruct methods. Only 1 is allowed."
            );
        }

        for (Method method : methods) {
            invoke(bean, method, "@PPostConstruct");
        }
    }

    private void invokePreDestroy(Object bean) {
        List<Method> methods = findAnnotatedMethods(bean.getClass(), PPreDestroy.class);
        for (Method method : methods) {
            invoke(bean, method, "@PPreDestroy");
        }
    }

    private void invoke(Object bean, Method method, String annotationName) {
        if (method.getParameterCount() > 0) {
            throw new FrameworkException(
                    annotationName + " method '" + method.getName() + "' in '" +
                    bean.getClass().getSimpleName() + "' must have no parameters."
            );
        }

        method.setAccessible(true);
        try {
            method.invoke(bean);
            logger.debug("{} invoked: {}#{}", annotationName,
                    bean.getClass().getSimpleName(), method.getName());
        } catch (Exception e) {
            throw new FrameworkException(
                    "Failed to invoke " + annotationName + " on '"
                    + bean.getClass().getSimpleName() + "#" + method.getName() + "'", e);
        }
    }

    private List<Method> findAnnotatedMethods(Class<?> clazz,
            Class<? extends java.lang.annotation.Annotation> annotation) {
        List<Method> result = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                result.add(method);
            }
        }
        return result;
    }
}
