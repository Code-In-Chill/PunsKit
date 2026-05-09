package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.config.POnConfigReload;
import com.punshub.punskit.annotation.di.PService;
import com.punshub.punskit.annotation.config.PValue;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PService
@Getter
public class ConfigTestService {

    private static final Logger log = LoggerFactory.getLogger(ConfigTestService.class);

    @PValue("${server.name}")
    private String serverName;

    @PValue("server.port")
    private int serverPort;

    @PValue("${database.enabled}")
    private boolean databaseEnabled;

    @PValue(value = "non.existent", defaultValue = "Default Value")
    private String nonExistent;

    @POnConfigReload
    public void onReload() {
        log.info("✓ [Config] Hot-reload hook executed! New server name: {}", serverName);
    }
}
