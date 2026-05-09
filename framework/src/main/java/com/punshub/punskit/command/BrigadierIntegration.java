package com.punshub.punskit.command;

import com.punshub.punskit.annotation.PCommand;
import com.punshub.punskit.logging.PunsLogger;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collection;

/**
 * Tích hợp hệ thống lệnh của PunsKit với Brigadier (PaperMC 1.20.6+).
 */
@RequiredArgsConstructor
public class BrigadierIntegration {

    private final JavaPlugin plugin;
    private final CommandManager commandManager;
    private final PunsLogger logger;

    /**
     * Đăng ký các bean lệnh vào hệ thống Brigadier của Paper.
     */
    public void registerCommands(Collection<Object> beans) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            int count = 0;
            for (Object bean : beans) {
                PCommand annotation = bean.getClass().getAnnotation(PCommand.class);
                if (annotation != null) {
                    registerBrigadier(commands, bean, annotation);
                    count++;
                }
            }
            
            if (count > 0) {
                logger.info("✓ Integrated {} command(s) with Brigadier (Paper Lifecycle).", count);
            }
        });
    }

    private void registerBrigadier(Commands registrar, Object bean, PCommand annotation) {
        String name = annotation.name();
        
        registrar.register(
            Commands.literal(name)
                .executes(ctx -> {
                    commandManager.execute(bean, annotation, ctx.getSource().getSender(), ctx.getInput());
                    return 1;
                })
                .build(),
            annotation.description(),
            Arrays.asList(annotation.aliases())
        );
        
        logger.debug("Registered Brigadier literal: /{}", name);
    }
}
