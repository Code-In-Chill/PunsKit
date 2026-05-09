package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.di.PPostConstruct;
import com.punshub.punskit.annotation.di.PService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@PService
@RequiredArgsConstructor
@Slf4j
public class GameManager {

    private final PlayerService playerService;
    private final AuthService authService;

    @PPostConstruct
    private void setup() {
        log.info("[GameManager] Ready! Dependency chain resolved successfully.");

        authService.authenticate("Notch");
        String result = playerService.getPlayerData("Notch");
        log.info("[GameManager] Test query result: {}", result);
    }
}
