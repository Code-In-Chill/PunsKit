package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.PostConstruct;
import com.punshub.punskit.annotation.PreDestroy;
import com.punshub.punskit.annotation.Service;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Bean đơn giản nhất — không phụ thuộc vào Bean nào khác.
 */
@Service
@NoArgsConstructor
@Slf4j
public class DatabaseService {

    @PostConstruct
    private void connect() {
        log.info("[DatabaseService] Connected to database. (PostConstruct ran)");
    }

    public String query(String sql) {
        return "Result of: " + sql;
    }

    @PreDestroy
    private void disconnect() {
        log.info("[DatabaseService] Disconnected from database. (PreDestroy ran)");
    }
}
