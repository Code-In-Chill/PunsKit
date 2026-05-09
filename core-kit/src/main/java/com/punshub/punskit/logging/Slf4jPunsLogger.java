package com.punshub.punskit.logging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.event.Level;

@RequiredArgsConstructor
public class Slf4jPunsLogger implements PunsLogger {

    private final Logger logger;
    private final String context;

    @Override
    public void info(String message, Object... args) {
        logger.info(format(message), args);
    }

    @Override
    public void debug(String message, Object... args) {
        logger.debug(format(message), args);
    }

    @Override
    public void warn(String message, Object... args) {
        logger.warn(format(message), args);
    }

    @Override
    public void error(String message, Object... args) {
        logger.error(format(message), args);
    }

    @Override
    public void error(String message, Throwable throwable, Object... args) {
        logger.error(format(message), args);
        logger.error("Exception details: ", throwable);
    }

    @Override
    public void log(Level level, String message, Object... args) {
        switch (level) {
            case INFO -> info(message, args);
            case DEBUG -> debug(message, args);
            case WARN -> warn(message, args);
            case ERROR -> error(message, args);
            case TRACE -> logger.trace(format(message), args);
        }
    }

    @Override
    public void log(Level level, String message, Throwable t, Object... args) {
        switch (level) {
            case INFO -> info(message, args);
            case DEBUG -> debug(message, args);
            case WARN -> warn(message, args);
            case ERROR -> error(message, t, args);
            case TRACE -> logger.trace(format(message), args, t);
        }
    }

    @Override
    public PunsLogger withContext(String context) {
        return new Slf4jPunsLogger(logger, this.context + " > " + context);
    }

    private String format(String message) {
        if (context == null || context.isEmpty()) {
            return message;
        }
        return "[" + context + "] " + message;
    }
}
