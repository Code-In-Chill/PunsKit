package com.punshub.punskit.platform.impl;

import com.punshub.punskit.command.CommandManager;
import com.punshub.punskit.logging.PunsLogger;
import com.punshub.punskit.platform.PlatformType;

import java.util.Collection;

/**
 * Adapter cơ bản cho Folia. Kế thừa LegacyAdapter tạm thời.
 * TODO: Implement regionized scheduling đầy đủ ở phase sau.
 */
public class FoliaAdapter extends LegacyAdapter {

    private final PunsLogger logger;

    public FoliaAdapter(CommandManager commandManager, PunsLogger logger) {
        super(commandManager);
        this.logger = logger;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.FOLIA;
    }

    @Override
    public void registerCommands(Collection<Class<?>> classes) {
        logger.warn("Running on Folia. Command registration is falling back to Legacy (CommandMap).");
        logger.warn("Note: Regionized multithreading is not fully supported yet in this adapter.");
        super.registerCommands(classes);
    }

    @Override
    public void onEnable(Collection<Object> beans) {
        super.onEnable(beans);
    }
}
