package com.punshub.punskit.scanner;

import com.punshub.punskit.annotation.Component;
import com.punshub.punskit.annotation.Service;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Quét JAR của plugin để tìm các class được đánh dấu {@code @Service} hoặc {@code @Component}.
 */
@RequiredArgsConstructor
public class ClasspathScanner {

    private final PunsLogger logger;

    public Set<Class<?>> scan(JavaPlugin plugin, String basePackage) {
        Set<Class<?>> candidates = new HashSet<>();
        ClassLoader classLoader = plugin.getClass().getClassLoader();
        URL jarUrl = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        String packagePath = basePackage.replace('.', '/');

        try {
            File jarFile = new File(jarUrl.toURI());
            logger.info("Scanning JAR: {} | package: {}", jarFile.getName(), basePackage);

            try (JarFile jar = new JarFile(jarFile)) {
                jar.stream()
                        .filter(entry ->
                                entry.getName().startsWith(packagePath)
                                && entry.getName().endsWith(".class")
                                && !entry.getName().contains("$")
                        )
                        .forEach(entry -> {
                            String className = entry.getName()
                                    .replace('/', '.')
                                    .replace(".class", "");

                            try {
                                Class<?> clazz = classLoader.loadClass(className);

                                if (isCandidate(clazz)) {
                                    candidates.add(clazz);
                                    logger.debug("Found candidate: {}", clazz.getSimpleName());
                                }
                            } catch (ClassNotFoundException e) {
                                logger.warn("Could not load class: {}", className);
                            }
                        });
            }

        } catch (URISyntaxException | java.io.IOException e) {
            throw new com.punshub.punskit.exception.FrameworkException(
                    "Failed to scan JAR for plugin: " + plugin.getName(), e);
        }

        logger.info("Scan complete. Found {} bean candidate(s).", candidates.size());
        return candidates;
    }

    private boolean isCandidate(Class<?> clazz) {
        if (clazz.isInterface()) return false;
        if (clazz.isAnnotation()) return false;
        if (java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) return false;

        return clazz.isAnnotationPresent(Service.class)
                || clazz.isAnnotationPresent(Component.class);
    }
}
