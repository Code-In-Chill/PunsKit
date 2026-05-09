package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.di.PScope;
import com.punshub.punskit.annotation.di.PScopeType;
import com.punshub.punskit.annotation.di.PService;
import lombok.extern.slf4j.Slf4j;

@PService
@PScope(PScopeType.PROTOTYPE)
@Slf4j
public class PrototypeService {
    public PrototypeService() {
        log.info("[PrototypeService] New instance created: {}", this.hashCode());
    }
}
