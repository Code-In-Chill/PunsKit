package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.PostConstruct;
import com.punshub.punskit.annotation.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final DatabaseService databaseService;
    private final MessageService messageService;

    @PostConstruct
    private void init() {
        log.info("[AuthService] Initialized with DB and Messages.");
    }

    public boolean authenticate(String user) {
        log.info("[AuthService] Authenticating {}", user);
        return true;
    }
}
