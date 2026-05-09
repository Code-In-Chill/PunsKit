package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.di.PPostConstruct;
import com.punshub.punskit.annotation.di.PPreDestroy;
import com.punshub.punskit.annotation.di.PService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Bean đơn giản nhất — không phụ thuộc vào Bean nào khác.
 */
@PService
@NoArgsConstructor
@Slf4j
public class DatabaseService {

    @PPostConstruct
    private void connect() {
        log.info("[DatabaseService] Connected to database. (PostConstruct ran)");
    }

    public String query(String sql) {
        return "Result of: " + sql;
    }

    @PPreDestroy
    private void disconnect() {
        log.info("[DatabaseService] Disconnected from database. (PreDestroy ran)");
    }
}
