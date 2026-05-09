package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.di.PPostConstruct;
import com.punshub.punskit.annotation.di.PService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@PService
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final DatabaseService databaseService;
    private final MessageService messageService;

    @PPostConstruct
    private void init() {
        log.info("[AuthService] Initialized with DB and Messages.");
    }

    public boolean authenticate(String user) {
        log.info("[AuthService] Authenticating {}", user);
        return true;
    }
}
