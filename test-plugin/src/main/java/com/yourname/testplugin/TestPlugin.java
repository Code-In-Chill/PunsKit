package com.yourname.testplugin;

import com.punshub.punskit.PunskitPlugin;
import com.yourname.testplugin.service.ConfigTestService;
import com.yourname.testplugin.service.PrototypeService;
import org.slf4j.Logger;

/**
 * Plugin mẫu để test framework sử dụng PunskitPlugin base class.
 */
public class TestPlugin extends PunskitPlugin {

    private Logger log;

    @Override
    public void onPluginEnable() {
        this.log = getSLF4JLogger();
        saveDefaultConfig();
        
        log.info("TestPlugin (PunskitPlugin) starting verification...");

        // Verification for Config Injection
        ConfigTestService configService = getBean(ConfigTestService.class);
        log.info("✓ Config Injection verified:");
        log.info("  - server.name: {}", configService.getServerName());
        log.info("  - server.port: {}", configService.getServerPort());
        log.info("  - database.enabled: {}", configService.isDatabaseEnabled());
        log.info("  - non.existent (default): {}", configService.getNonExistent());

        // Verification for Prototype Scope
        // Sử dụng getBean() trực tiếp từ PunskitPlugin
        PrototypeService p1 = getBean(PrototypeService.class);
        PrototypeService p2 = getBean(PrototypeService.class);
        
        if (p1 != p2) {
            log.info("✓ Prototype Scope verified: p1 and p2 are different instances.");
        } else {
            log.error("✗ Prototype Scope failed: p1 and p2 are the same instance.");
        }

        log.info("TestPlugin enabled successfully!");
    }

    @Override
    public void onPluginDisable() {
        if (log != null) {
            log.info("TestPlugin disabling...");
        }
    }
}
