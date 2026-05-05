package com.yourname.testplugin;

import com.punshub.punskit.FrameworkLauncher;
import com.yourname.testplugin.service.PrototypeService;
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

        // Verification for Prototype Scope
        PrototypeService p1 = framework.getBean(PrototypeService.class);
        PrototypeService p2 = framework.getBean(PrototypeService.class);
        
        if (p1 != p2) {
            log.info("✓ Prototype Scope verified: p1 and p2 are different instances.");
        } else {
            log.error("✗ Prototype Scope failed: p1 and p2 are the same instance.");
        }

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
