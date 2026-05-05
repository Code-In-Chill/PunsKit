package com.yourname.testplugin;

import com.punshub.punskit.FrameworkLauncher;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

/**
 * Plugin mẫu để test framework.
 */
public class TestPlugin extends JavaPlugin {

    private FrameworkLauncher framework;
    private Logger log;

    @Override
    public void onEnable() {
        this.log = getSLF4JLogger();
        
        // ── Một dòng duy nhất để khởi động toàn bộ IoC Container ─────────────
        framework = FrameworkLauncher.start(this, "com.yourname.testplugin");

        log.info("TestPlugin enabled!");
    }

    @Override
    public void onDisable() {
        if (framework != null) {
            framework.shutdown();
        }
        log.info("TestPlugin disabled!");
    }
}
