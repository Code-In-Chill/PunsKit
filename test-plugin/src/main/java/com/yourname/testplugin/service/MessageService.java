package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.di.PPostConstruct;
import com.punshub.punskit.annotation.di.PService;
import lombok.extern.slf4j.Slf4j;

@PService
@Slf4j
public class MessageService {

    @PPostConstruct
    private void init() {
        log.info("[MessageService] Initialized.");
    }

    public String getWelcomeMessage() {
        return "Welcome to PunsHub!";
    }
}
