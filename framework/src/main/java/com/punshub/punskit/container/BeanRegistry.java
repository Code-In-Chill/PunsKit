package com.punshub.punskit.container;

import com.punshub.punskit.annotation.*;
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
import java.util.stream.Collectors;

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
            collectInterfaces(candidate).forEach(iface ->
                    interfaceMap.computeIfAbsent(iface, k -> new ArrayList<>()).add(candidate)
            );
        }
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
        if (!type.isAnnotationPresent(Scope.class)) {
            return true;
        }
        return type.getAnnotation(Scope.class).value() == ScopeType.SINGLETON;
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
            if (e instanceof com.punshub.punskit.exception.FrameworkException fe) throw fe;
            throw new com.punshub.punskit.exception.FrameworkException(
                    "Failed to create bean: " + type.getSimpleName(), e);
        } finally {
            currentlyResolving.remove(type);
        }
    }

    private Constructor<?> findConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        for (Constructor<?> ctor : constructors) {
            if (ctor.isAnnotationPresent(Autowired.class)) {
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
                " constructors but none is annotated with @Autowired."
        );
    }

    private Object[] resolveConstructorArgs(Constructor<?> constructor) {
        Parameter[] params = constructor.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> paramType = params[i].getType();
            Qualifier qualifier = params[i].getAnnotation(Qualifier.class);
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
        List<Class<?>> impls = interfaceMap.getOrDefault(interfaceType, Collections.emptyList());
        if (impls.isEmpty()) {
            throw new BeanNotFoundException(interfaceType);
        }
        if (qualifierName != null) {
            List<Class<?>> qualified = impls.stream()
                    .filter(impl -> {
                        Qualifier q = impl.getAnnotation(Qualifier.class);
                        return q != null && q.value().equals(qualifierName);
                    })
                    .collect(Collectors.toList());
            if (qualified.size() == 1) return resolve((Class<T>) qualified.get(0));
            throw new BeanNotFoundException(interfaceType);
        }
        if (impls.size() == 1) {
            return resolve((Class<T>) impls.get(0));
        }
        List<Class<?>> primaryImpls = impls.stream()
                .filter(impl -> impl.isAnnotationPresent(Primary.class))
                .collect(Collectors.toList());
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
