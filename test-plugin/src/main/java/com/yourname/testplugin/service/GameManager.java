package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.PostConstruct;
import com.punshub.punskit.annotation.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameManager {

    private final PlayerService playerService;
    private final AuthService authService;

    @PostConstruct
    private void setup() {
        log.info("[GameManager] Ready! Dependency chain resolved successfully.");

        authService.authenticate("Notch");
        String result = playerService.getPlayerData("Notch");
        log.info("[GameManager] Test query result: {}", result);
    }
}
