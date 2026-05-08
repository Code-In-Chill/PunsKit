package com.punshub.punskit;

import com.punshub.punskit.command.CommandManager;
import com.punshub.punskit.container.BeanRegistry;
import com.punshub.punskit.lifecycle.LifecycleManager;
import com.punshub.punskit.logging.PunsLogger;
import com.punshub.punskit.logging.Slf4jPunsLogger;
import com.punshub.punskit.scanner.ClasspathScanner;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Set;

/**
 * Entry point của framework — khởi động và tắt IoC Container.
 */
public class FrameworkLauncher {

    private final JavaPlugin plugin;
    @Getter
    private final BeanRegistry registry;
    private final LifecycleManager lifecycleManager;
    private final CommandManager commandManager;
    private final PunsLogger logger;

    private FrameworkLauncher(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = new Slf4jPunsLogger(plugin.getSLF4JLogger(), "PunsKit");
        this.registry = new BeanRegistry(logger.withContext("Registry"));
        this.lifecycleManager = new LifecycleManager(logger.withContext("Lifecycle"));
        this.commandManager = new CommandManager(plugin, logger.withContext("Command"));
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

        Collection<Object> singletonBeans = registry.getAllBeans();
        lifecycleManager.invokePostConstructAll(singletonBeans);
        
        registerListeners(singletonBeans);
        commandManager.registerCommands(singletonBeans);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("IoC Container started. {} bean(s) registered in {}ms.",
                candidates.size(), elapsed);
    }

    private void registerListeners(Collection<Object> beans) {
        int count = 0;
        for (Object bean : beans) {
            if (bean instanceof Listener listener) {
                Bukkit.getPluginManager().registerEvents(listener, plugin);
                logger.debug("Auto-registered listener: {}", bean.getClass().getSimpleName());
                count++;
            }
        }
        if (count > 0) {
            logger.info("✓ Auto-registered {} event listener(s).", count);
        }
    }

    public void shutdown() {
        logger.info("Shutting down IoC Container...");
        
        Collection<Object> singletonBeans = registry.getAllBeans();
        unregisterListeners(singletonBeans);
        
        lifecycleManager.invokePreDestroyAll(singletonBeans);
        logger.info("IoC Container shut down cleanly.");
    }

    private void unregisterListeners(Collection<Object> beans) {
        int count = 0;
        for (Object bean : beans) {
            if (bean instanceof Listener listener) {
                HandlerList.unregisterAll(listener);
                count++;
            }
        }
        if (count > 0) {
            logger.debug("Unregistered {} event listener(s).", count);
        }
    }

    public <T> T getBean(Class<T> type) {
        return registry.getBean(type);
    }
}
