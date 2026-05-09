package com.punshub.punskit;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base class for plugins using PunsKit.
 * Handles the IoC container lifecycle automatically.
 */
public abstract class PunskitPlugin extends JavaPlugin {

    private FrameworkLauncher launcher;
    private String basePackage;

    /**
     * Sets the base package to scan for components.
     * Best called in the constructor or {@code onLoad()}.
     * If not set, it defaults to the package of the plugin class.
     *
     * @param basePackage The package to scan.
     */
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    public final void onLoad() {
        // 1. Trigger user-defined load logic (Allows setting basePackage, etc.)
        onPluginLoad();

        // 2. Auto-detect package if not manually set
        if (this.basePackage == null) {
            this.basePackage = this.getClass().getPackage().getName();
        }

        // 3. Bootstrap the framework (required for Brigadier command registration)
        this.launcher = FrameworkLauncher.bootstrap(this, this.basePackage);
    }

    @Override
    public final void onEnable() {
        // 1. Finalize framework initialization (DI, Listeners, Schedulers)
        if (this.launcher != null) {
            this.launcher.initialize();
        }

        // 2. Trigger user-defined enable logic
        onPluginEnable();
    }

    /**
     * Called when the plugin is loaded (onLoad).
     */
    public void onPluginLoad() {
    }

    @Override
    public final void onDisable() {
        // 1. Trigger user-defined disable logic
        onPluginDisable();

        // 2. Shutdown the framework
        if (this.launcher != null) {
            this.launcher.shutdown();
        }
    }

    /**
     * Reloads the plugin configuration and re-injects all @PValue fields.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (launcher != null) {
            launcher.reloadConfig();
        }
    }

    /**
     * Called when the plugin is enabled and the IoC container is ready.
     */
    public void onPluginEnable() {
    }

    /**
     * Called when the plugin is disabling, before the IoC container is shut down.
     */
    public void onPluginDisable() {
    }

    /**
     * Gets a bean from the IoC container.
     *
     * @param type The class of the bean to retrieve.
     * @param <T>  The type of the bean.
     * @return The bean instance.
     * @throws com.punshub.punskit.exception.BeanNotFoundException if no bean is found.
     */
    public <T> T getBean(Class<T> type) {
        if (launcher == null) {
            throw new IllegalStateException("Framework is not initialized. getBean() can only be used after onEnable().");
        }
        return launcher.getBean(type);
    }
}
