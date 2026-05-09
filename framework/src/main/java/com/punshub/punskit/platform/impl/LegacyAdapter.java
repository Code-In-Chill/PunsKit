package com.punshub.punskit.platform.impl;

import com.punshub.punskit.command.CommandManager;
import com.punshub.punskit.platform.PlatformAdapter;
import com.punshub.punskit.platform.PlatformType;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * Adapter cho các nền tảng cũ hoặc Spigot thuần (Sử dụng CommandMap).
 */
@RequiredArgsConstructor
public class LegacyAdapter implements PlatformAdapter {

    protected final CommandManager commandManager;

    @Override
    public PlatformType getType() {
        return PlatformType.LEGACY;
    }

    @Override
    public void registerCommands(Collection<Object> beans) {
        commandManager.registerCommands(beans);
    }

    @Override
    public void unregisterCommands() {
        commandManager.cleanup();
    }

    @Override
    public boolean isBrigadierSupported() {
        return false;
    }
}
