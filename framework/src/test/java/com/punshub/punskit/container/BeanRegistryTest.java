package com.punshub.punskit.container;

import com.punshub.punskit.annotation.di.PScope;
import com.punshub.punskit.annotation.di.PPScopeType;
import com.punshub.punskit.annotation.di.PService;
import com.punshub.punskit.logging.PunsLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BeanRegistryTest {

    private BeanRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BeanRegistry(new DummyLogger());
    }

    @PService
    static class SingletonBean {}

    @PService
    @PScope(PScopeType.PROTOTYPE)
    static class PrototypeBean {}

    @Test
    void testSingletonScope() {
        registry.registerCandidates(Set.of(SingletonBean.class));
        
        SingletonBean b1 = registry.resolve(SingletonBean.class);
        SingletonBean b2 = registry.resolve(SingletonBean.class);
        
        assertSame(b1, b2, "Singleton beans should be the same instance");
    }

    @Test
    void testPrototypeScope() {
        registry.registerCandidates(Set.of(PrototypeBean.class));
        
        PrototypeBean b1 = registry.resolve(PrototypeBean.class);
        PrototypeBean b2 = registry.resolve(PrototypeBean.class);
        
        assertNotSame(b1, b2, "Prototype beans should be different instances");
    }

    static class DummyLogger implements PunsLogger {
        @Override public void info(String message, Object... args) {}
        @Override public void debug(String message, Object... args) {}
        @Override public void warn(String message, Object... args) {}
        @Override public void error(String message, Object... args) {}
        @Override public void error(String message, Throwable throwable, Object... args) {}
        @Override public void log(Level level, String message, Object... args) {}
        @Override public void log(Level level, String message, Throwable t, Object... args) {}
        @Override public PunsLogger withContext(String context) { return this; }
    }
}
