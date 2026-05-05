package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.PostConstruct;
import com.punshub.punskit.annotation.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageService {

    @PostConstruct
    private void init() {
        log.info("[MessageService] Initialized.");
    }

    public String getWelcomeMessage() {
        return "Welcome to PunsHub!";
    }
}
