package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.OnConfigReload;
import com.punshub.punskit.annotation.Service;
import com.punshub.punskit.annotation.Value;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Getter
public class ConfigTestService {

    private static final Logger log = LoggerFactory.getLogger(ConfigTestService.class);

    @Value("${server.name}")
    private String serverName;

    @Value("server.port")
    private int serverPort;

    @Value("${database.enabled}")
    private boolean databaseEnabled;

    @Value(value = "non.existent", defaultValue = "Default Value")
    private String nonExistent;

    @OnConfigReload
    public void onReload() {
        log.info("✓ [Config] Hot-reload hook executed! New server name: {}", serverName);
    }
}
