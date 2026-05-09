package com.punshub.punskit.scanner;

import com.punshub.punskit.annotation.Component;
import com.punshub.punskit.annotation.Service;
import com.punshub.punskit.logging.PunsLogger;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quét classpath của plugin sử dụng ClassGraph để tìm các bean candidates.
 */
@RequiredArgsConstructor
public class ClasspathScanner {

    private final PunsLogger logger;

    /**
     * Quét các class được đánh dấu {@code @Service} hoặc {@code @Component} trong package chỉ định.
     *
     * @param plugin      Plugin đang chạy.
     * @param basePackage Package cơ sở để quét.
     * @return Tập hợp các class thỏa mãn điều kiện.
     */
    public Set<Class<?>> scan(JavaPlugin plugin, String basePackage) {
        logger.info("Scanning package: {} using ClassGraph", basePackage);
        long startTime = System.currentTimeMillis();

        ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .ignoreParentModuleLayers() // Tránh quét các module của JVM
                .overrideClassLoaders(pluginClassLoader) // Chỉ quét ClassLoader của plugin này
                .acceptPackages(basePackage)
                .scan()) {

            Set<Class<?>> candidates = scanResult.getClassesWithAnnotation(Service.class.getName())
                    .union(scanResult.getClassesWithAnnotation(Component.class.getName()))
                    .stream()
                    .filter(ci -> !ci.isInterface() && !ci.isAbstract())
                    .map(ci -> {
                        try {
                            return pluginClassLoader.loadClass(ci.getName());
                        } catch (ClassNotFoundException e) {
                            logger.warn("Could not load scanned class: {}", ci.getName());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Scan complete. Found {} candidate(s) in {}ms.", candidates.size(), elapsed);
            return candidates;
        }
    }
}
