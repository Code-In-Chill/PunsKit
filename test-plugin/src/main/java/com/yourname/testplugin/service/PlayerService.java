package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.PostConstruct;
import com.punshub.punskit.annotation.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final DatabaseService databaseService;

    @PostConstruct
    private void init() {
        log.info("[PlayerService] Initialized. Can access DB: {}", 
                (databaseService != null ? "YES" : "NO"));
    }

    public String getPlayerData(String playerName) {
        return databaseService.query("SELECT * FROM players WHERE name = '" + playerName + "'");
    }
}
