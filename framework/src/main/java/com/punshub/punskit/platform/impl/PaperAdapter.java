package com.punshub.punskit.platform.impl;

import com.punshub.punskit.command.BrigadierIntegration;
import com.punshub.punskit.command.CommandManager;
import com.punshub.punskit.platform.PlatformType;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * Adapter cho Paper Modern (1.20.6+).
 */
public class PaperAdapter extends LegacyAdapter {

    private final BrigadierIntegration brigadier;

    public PaperAdapter(CommandManager commandManager, BrigadierIntegration brigadier) {
        super(commandManager);
        this.brigadier = brigadier;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.PAPER;
    }

    @Override
    public void registerCommands(Collection<Object> beans) {
        // Trên Paper, ta ưu tiên dùng Brigadier thông qua Lifecycle API
        brigadier.registerCommands(beans);
    }

    @Override
    public boolean isBrigadierSupported() {
        return true;
    }
}
