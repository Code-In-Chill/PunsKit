package com.punshub.punskit.logging;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Enterprise Logging Interface for PunsKit.
 * Provides structured logging and automatic context management.
 */
public interface PunsLogger {

    void info(String message, Object... args);

    void debug(String message, Object... args);

    void warn(String message, Object... args);

    void error(String message, Object... args);

    void error(String message, Throwable throwable, Object... args);

    void log(Level level, String message, Object... args);

    void log(Level level, String message, Throwable t, Object... args);

    /**
     * Creates a child logger with a specific context/prefix.
     */
    PunsLogger withContext(String context);
}
