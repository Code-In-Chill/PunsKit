package com.punshub.punskit.container;

import com.punshub.punskit.annotation.di.*;
import com.punshub.punskit.config.ConfigInjector;
import com.punshub.punskit.exception.AmbiguousBeanException;
import com.punshub.punskit.exception.BeanNotFoundException;
import com.punshub.punskit.exception.CircularDependencyException;
import com.punshub.punskit.logging.PunsLogger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Trung tâm lưu trữ và giải quyết phụ thuộc của IoC Container.
 */
@RequiredArgsConstructor
public class BeanRegistry {

    private final LinkedHashMap<Class<?>, Object> beans = new LinkedHashMap<>();
    private final Map<Class<?>, List<Class<?>>> interfaceMap = new HashMap<>();
    private final LinkedHashSet<Class<?>> currentlyResolving = new LinkedHashSet<>();
    private Set<Class<?>> allCandidates = new HashSet<>();

    private final PunsLogger logger;
    @Getter
    private ConfigInjector configInjector;

    public void setConfigInjector(ConfigInjector injector) {
        this.configInjector = injector;
    }

    public void registerCandidates(Set<Class<?>> candidates) {
        this.allCandidates = candidates;
        for (Class<?> candidate : candidates) {
            Set<Class<?>> interfaces = collectInterfaces(candidate);
            for (Class<?> iface : interfaces) {
                if (!interfaceMap.containsKey(iface)) {
                    interfaceMap.put(iface, new ArrayList<>());
                }
                interfaceMap.get(iface).add(candidate);
            }
        }
    }

    public Collection<Class<?>> getAllCandidates() {
        return Collections.unmodifiableCollection(allCandidates);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        if (isSingleton(type) && beans.containsKey(type)) {
            return (T) beans.get(type);
        }
        if (type.isInterface() || java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            return resolveByInterface(type, null);
        }
        return createBean(type);
    }

    private boolean isSingleton(Class<?> type) {
        if (!type.isAnnotationPresent(PScope.class)) {
            return true;
        }
        return type.getAnnotation(PScope.class).value() == PScopeType.SINGLETON;
    }

    @SuppressWarnings("unchecked")
    public <T> T resolveWithQualifier(Class<T> type, String qualifierName) {
        if (type.isInterface() || java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            return resolveByInterface(type, qualifierName);
        }
        return resolve(type);
    }

    @SuppressWarnings("unchecked")
    private <T> T createBean(Class<T> type) {
        if (currentlyResolving.contains(type)) {
            List<Class<?>> path = new ArrayList<>(currentlyResolving);
            throw new CircularDependencyException(path, type);
        }

        currentlyResolving.add(type);
        logger.debug("Creating {} bean: {}", isSingleton(type) ? "singleton" : "prototype", type.getSimpleName());

        try {
            Constructor<?> constructor = findConstructor(type);
            Object[] args = resolveConstructorArgs(constructor);

            T instance = (T) constructor.newInstance(args);
            
            // Perform post-instantiation injection
            if (configInjector != null) {
                configInjector.inject(instance);
            }

            if (isSingleton(type)) {
                beans.put(type, instance);
                logger.info("✓ Created singleton bean: {}", type.getSimpleName());
            } else {
                logger.debug("✓ Created prototype bean: {}", type.getSimpleName());
            }

            return instance;

        } catch (Exception e) {
            if (e instanceof com.punshub.punskit.exception.FrameworkException) {
                throw (com.punshub.punskit.exception.FrameworkException) e;
            }
            throw new com.punshub.punskit.exception.FrameworkException(
                    "Failed to create bean: " + type.getSimpleName(), e);
        } finally {
            currentlyResolving.remove(type);
        }
    }

    private Constructor<?> findConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        for (Constructor<?> ctor : constructors) {
            if (ctor.isAnnotationPresent(PAutowired.class)) {
                ctor.setAccessible(true);
                return ctor;
            }
        }
        if (constructors.length == 1) {
            constructors[0].setAccessible(true);
            return constructors[0];
        }
        throw new com.punshub.punskit.exception.FrameworkException(
                "Bean '" + type.getSimpleName() + "' has " + constructors.length +
                " constructors but none is annotated with @PAutowired."
        );
    }

    private Object[] resolveConstructorArgs(Constructor<?> constructor) {
        Parameter[] params = constructor.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> paramType = params[i].getType();
            PQualifier qualifier = params[i].getAnnotation(PQualifier.class);
            if (qualifier != null) {
                args[i] = resolveWithQualifier(paramType, qualifier.value());
            } else {
                args[i] = resolve(paramType);
            }
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveByInterface(Class<T> interfaceType, String qualifierName) {
        List<Class<?>> impls = interfaceMap.get(interfaceType);
        if (impls == null || impls.isEmpty()) {
            throw new BeanNotFoundException(interfaceType);
        }

        if (qualifierName != null) {
            for (Class<?> impl : impls) {
                PQualifier q = impl.getAnnotation(PQualifier.class);
                if (q != null && q.value().equals(qualifierName)) {
                    return resolve((Class<T>) impl);
                }
            }
            throw new BeanNotFoundException(interfaceType);
        }

        if (impls.size() == 1) {
            return resolve((Class<T>) impls.get(0));
        }

        List<Class<?>> primaryImpls = new ArrayList<>();
        for (Class<?> impl : impls) {
            if (impl.isAnnotationPresent(PPrimary.class)) {
                primaryImpls.add(impl);
            }
        }

        if (primaryImpls.size() == 1) {
            return resolve((Class<T>) primaryImpls.get(0));
        }
        throw new AmbiguousBeanException(interfaceType, impls);
    }

    private Set<Class<?>> collectInterfaces(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Class<?> iface : current.getInterfaces()) {
                result.add(iface);
                result.addAll(collectInterfaces(iface));
            }
            current = current.getSuperclass();
        }
        return result;
    }

    public Collection<Object> getAllBeans() {
        return Collections.unmodifiableCollection(beans.values());
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        return (T) beans.get(type);
    }

    public boolean containsBean(Class<?> type) {
        return beans.containsKey(type);
    }
}
