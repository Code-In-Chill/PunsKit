package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.Service;
import com.punshub.punskit.annotation.Value;
import lombok.Getter;

@Service
@Getter
public class ConfigTestService {

    @Value("${server.name}")
    private String serverName;

    @Value("server.port")
    private int serverPort;

    @Value("${database.enabled}")
    private boolean databaseEnabled;

    @Value(value = "non.existent", defaultValue = "Default Value")
    private String nonExistent;
}
