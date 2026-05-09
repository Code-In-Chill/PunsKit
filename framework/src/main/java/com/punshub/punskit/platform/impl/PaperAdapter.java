package com.punshub.punskit.platform.impl;

import com.punshub.punskit.command.BrigadierIntegration;
import com.punshub.punskit.command.CommandManager;
import com.punshub.punskit.platform.PlatformType;

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
    public void registerCommands(Collection<Class<?>> classes) {
        // Trên Paper, ta ưu tiên dùng Brigadier thông qua Lifecycle API
        brigadier.registerCommands(classes);
    }

    @Override
    public void onEnable(Collection<Object> beans) {
        // Đã đăng ký qua Brigadier trong bootstrap/onLoad, không cần làm gì thêm ở đây.
    }

    @Override
    public boolean isBrigadierSupported() {
        return true;
    }
}
