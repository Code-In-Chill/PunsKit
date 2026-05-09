package com.punshub.punskit;

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
import com.punshub.punskit.platform.impl.LegacyAdapter;
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

    private FrameworkLauncher(JavaPlugin plugin, String basePackage) {
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
        try {
            return switch (type) {
                case PAPER -> {
                    Class<?> adapterClass = Class.forName("com.punshub.punskit.platform.impl.PaperAdapter");
                    Class<?> brigadierClass = Class.forName("com.punshub.punskit.command.BrigadierIntegration");
                    Object brigadier = brigadierClass.getConstructor(JavaPlugin.class, CommandManager.class, BeanRegistry.class, PunsLogger.class)
                            .newInstance(plugin, commandManager, registry, logger.withContext("Brigadier"));
                    yield (PlatformAdapter) adapterClass.getConstructor(CommandManager.class, brigadierClass)
                            .newInstance(commandManager, brigadier);
                }
                case FOLIA -> {
                    Class<?> adapterClass = Class.forName("com.punshub.punskit.platform.impl.FoliaAdapter");
                    yield (PlatformAdapter) adapterClass.getConstructor(CommandManager.class, PunsLogger.class)
                            .newInstance(commandManager, logger.withContext("Folia"));
                }
                default -> new LegacyAdapter(commandManager);
            };
        } catch (Exception e) {
            logger.error("Failed to initialize platform adapter for {}. Falling back to Legacy.", type, e);
            return new LegacyAdapter(commandManager);
        }
    }

    public static FrameworkLauncher bootstrap(JavaPlugin plugin, String basePackage) {
        FrameworkLauncher launcher = new FrameworkLauncher(plugin, basePackage);
        launcher.bootstrap(basePackage);
        return launcher;
    }

    private void bootstrap(String basePackage) {
        long startTime = System.currentTimeMillis();
        logger.info("Bootstrapping IoC Container...");

        ClasspathScanner scanner = new ClasspathScanner(logger.withContext("Scanner"));
        Set<Class<?>> candidates = scanner.scan(plugin, basePackage);

        if (candidates.isEmpty()) {
            logger.warn("No bean candidates found in package: {}", basePackage);
            return;
        }

        registry.registerCandidates(candidates);
        
        // Command registration (Paper will register Brigadier nodes here)
        platformAdapter.registerCommands(candidates);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("IoC Container bootstrapped in {}ms.", elapsed);
    }

    public void initialize() {
        long startTime = System.currentTimeMillis();
        logger.info("Finalizing IoC Container initialization...");

        Collection<Class<?>> candidates = registry.getAllCandidates();
        for (Class<?> candidate : candidates) {
            if (!registry.containsBean(candidate)) {
                registry.resolve(candidate);
            }
        }

        Collection<Object> singletonBeans = registry.getAllBeans();
        lifecycleManager.invokePostConstructAll(singletonBeans);
        
        conditionRegistry.registerProviders(singletonBeans);
        registerListeners(singletonBeans);
        
        // Post-initialization platform hook (Legacy will register commands here)
        platformAdapter.onEnable(singletonBeans);
        
        schedulerManager.registerSchedulers(singletonBeans);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("IoC Container initialized. {} bean(s) registered in {}ms.",
                singletonBeans.size(), elapsed);
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
            Collection<Object> beans = registry.getAllBeans();
            injector.reinjectAll(beans);
            lifecycleManager.invokePostConstructOnReload(beans);
        }
    }


}
