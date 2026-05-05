package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.Scope;
import com.punshub.punskit.annotation.ScopeType;
import com.punshub.punskit.annotation.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Scope(ScopeType.PROTOTYPE)
@Slf4j
public class PrototypeService {
    public PrototypeService() {
        log.info("[PrototypeService] New instance created: {}", this.hashCode());
    }
}
