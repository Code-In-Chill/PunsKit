package com.punshub.punskit;

import com.punshub.punskit.command.BrigadierIntegration;
import com.punshub.punskit.command.CommandManager;
import com.punshub.punskit.command.ConditionRegistry;
import com.punshub.punskit.config.ConfigInjector;
import com.punshub.punskit.container.BeanRegistry;
import com.punshub.punskit.lifecycle.LifecycleManager;
import com.punshub.punskit.logging.PunsLogger;
import com.punshub.punskit.logging.Slf4jPunsLogger;
import com.punshub.punskit.platform.PlatformAdapter;
import com.punshub.punskit.platform.PlatformDetector;
import com.punshub.punskit.platform.PlatformType;
import com.punshub.punskit.platform.impl.FoliaAdapter;
import com.punshub.punskit.platform.impl.LegacyAdapter;
import com.punshub.punskit.platform.impl.PaperAdapter;
import com.punshub.punskit.scanner.ClasspathScanner;
import com.punshub.punskit.scheduler.SchedulerManager;
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
    private final ConditionRegistry conditionRegistry;
    private final SchedulerManager schedulerManager;
    private final PunsLogger logger;
    private final PlatformAdapter platformAdapter;

    private FrameworkLauncher(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = new Slf4jPunsLogger(plugin.getSLF4JLogger(), "PunsKit");
        this.registry = new BeanRegistry(logger.withContext("Registry"));
        this.lifecycleManager = new LifecycleManager(logger.withContext("Lifecycle"));
        this.conditionRegistry = new ConditionRegistry(logger.withContext("Condition"));
        this.commandManager = new CommandManager(plugin, conditionRegistry, logger.withContext("Command"));
        this.schedulerManager = new SchedulerManager(plugin, logger.withContext("Scheduler"));

        // Platform Detection and Initialization
        this.platformAdapter = createPlatformAdapter();
        logger.info("Platform initialized: {}", platformAdapter.getType());

        ConfigInjector configInjector = new ConfigInjector(plugin, logger.withContext("Config"));
        this.registry.setConfigInjector(configInjector);
    }

    private PlatformAdapter createPlatformAdapter() {
        PlatformType type = PlatformDetector.detect(logger);
        return switch (type) {
            case PAPER -> new PaperAdapter(
                    commandManager,
                    new BrigadierIntegration(plugin, commandManager, logger.withContext("Brigadier"))
            );
            case FOLIA -> new FoliaAdapter(commandManager, logger.withContext("Folia"));
            default -> new LegacyAdapter(commandManager);
        };
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
        
        conditionRegistry.registerProviders(singletonBeans);
        registerListeners(singletonBeans);
        
        // Use PlatformAdapter instead of direct commandManager call
        platformAdapter.registerCommands(singletonBeans);
        
        schedulerManager.registerSchedulers(singletonBeans);

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
        schedulerManager.shutdown();
        
        // Use PlatformAdapter for cleanup
        platformAdapter.unregisterCommands();
        
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

    public void reloadConfig() {
        ConfigInjector injector = registry.getConfigInjector();
        if (injector != null) {
            injector.reinjectAll(registry.getAllBeans());
        }
    }


}
