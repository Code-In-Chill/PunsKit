package com.punshub.punskit;

import com.punshub.punskit.container.BeanRegistry;
import com.punshub.punskit.lifecycle.LifecycleManager;
import com.punshub.punskit.logging.PunsLogger;
import com.punshub.punskit.logging.Slf4jPunsLogger;
import com.punshub.punskit.scanner.ClasspathScanner;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/**
 * Entry point của framework — khởi động và tắt IoC Container.
 */
public class FrameworkLauncher {

    private final JavaPlugin plugin;
    @Getter
    private final BeanRegistry registry;
    private final LifecycleManager lifecycleManager;
    private final PunsLogger logger;

    private FrameworkLauncher(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = new Slf4jPunsLogger(plugin.getSLF4JLogger(), "PunsKit");
        this.registry = new BeanRegistry(logger.withContext("Registry"));
        this.lifecycleManager = new LifecycleManager(logger.withContext("Lifecycle"));
    }

    public static FrameworkLauncher start(JavaPlugin plugin, String basePackage) {
        FrameworkLauncher launcher = new FrameworkLauncher(plugin);
        launcher.initialize(basePackage);
        return launcher;
    }

    private void initialize(String basePackage) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting IoC Container...");

        ClasspathScanner scanner = new ClasspathScanner(logger.withContext("Scanner"));
        Set<Class<?>> candidates = scanner.scan(plugin, basePackage);

        if (candidates.isEmpty()) {
            logger.warn("No bean candidates found in package: {}", basePackage);
            return;
        }

        registry.registerCandidates(candidates);

        for (Class<?> candidate : candidates) {
            if (!registry.containsBean(candidate)) {
                registry.resolve(candidate);
            }
        }

        lifecycleManager.invokePostConstructAll(registry.getAllBeans());

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("IoC Container started. {} bean(s) registered in {}ms.",
                candidates.size(), elapsed);
    }

    public void shutdown() {
        logger.info("Shutting down IoC Container...");
        lifecycleManager.invokePreDestroyAll(registry.getAllBeans());
        logger.info("IoC Container shut down cleanly.");
    }

    public <T> T getBean(Class<T> type) {
        return registry.getBean(type);
    }
}
