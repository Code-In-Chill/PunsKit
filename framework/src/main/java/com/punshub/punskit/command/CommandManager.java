package com.punshub.punskit.command;

import com.punshub.punskit.annotation.Command;
import com.punshub.punskit.annotation.CommandHandler;
import com.punshub.punskit.annotation.Subcommand;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Quản lý đăng ký và điều hướng lệnh (Command Routing).
 */
@RequiredArgsConstructor
public class CommandManager {

    private final JavaPlugin plugin;
    private final PunsLogger logger;
    private CommandMap commandMap;

    public void registerCommands(Collection<Object> beans) {
        if (commandMap == null) {
            initializeCommandMap();
        }

        int count = 0;
        for (Object bean : beans) {
            Command annotation = bean.getClass().getAnnotation(Command.class);
            if (annotation != null) {
                registerCommand(bean, annotation);
                count++;
            }
        }

        if (count > 0) {
            logger.info("✓ Auto-registered {} command(s).", count);
        }
    }

    private void initializeCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            this.commandMap = (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            logger.error("Failed to access Bukkit CommandMap", e);
        }
    }

    private void registerCommand(Object bean, Command annotation) {
        String name = annotation.name();
        PunsWrappedCommand wrappedCommand = new PunsWrappedCommand(name, bean, annotation, logger);
        
        commandMap.register(plugin.getName().toLowerCase(), wrappedCommand);
        logger.debug("Registered dynamic command: /{}", name);
    }

    private static class PunsWrappedCommand extends BukkitCommand {
        private final Object bean;
        private final PunsLogger logger;
        private Method mainHandler;
        private final Map<String, Method> subcommands = new HashMap<>();

        protected PunsWrappedCommand(String name, Object bean, Command annotation, PunsLogger logger) {
            super(name);
            this.bean = bean;
            this.logger = logger.withContext("Command:" + name);
            
            setLabel(name);
            setDescription(annotation.description());
            setPermission(annotation.permission().isEmpty() ? null : annotation.permission());
            setAliases(Arrays.asList(annotation.aliases()));

            parseHandlers();
        }

        private void parseHandlers() {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(CommandHandler.class)) {
                    this.mainHandler = method;
                    this.mainHandler.setAccessible(true);
                } else if (method.isAnnotationPresent(Subcommand.class)) {
                    Subcommand sub = method.getAnnotation(Subcommand.class);
                    method.setAccessible(true);
                    subcommands.put(sub.value().toLowerCase(), method);
                }
            }
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            if (getPermission() != null && !sender.hasPermission(getPermission())) {
                sender.sendMessage("§cYou do not have permission to execute this command.");
                return true;
            }

            try {
                if (args.length > 0) {
                    Method subHandler = subcommands.get(args[0].toLowerCase());
                    if (subHandler != null) {
                        invokeHandler(subHandler, sender, args);
                        return true;
                    }
                }

                if (mainHandler != null) {
                    invokeHandler(mainHandler, sender, args);
                } else {
                    sender.sendMessage("§cUsage: /" + getName() + " [" + String.join("|", subcommands.keySet()) + "]");
                }
            } catch (Exception e) {
                logger.error("Error executing command /" + getName(), e);
                sender.sendMessage("§cAn internal error occurred while executing this command.");
            }
            return true;
        }

        private void invokeHandler(Method method, CommandSender sender, String[] args) throws Exception {
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                if (CommandSender.class.isAssignableFrom(paramTypes[i])) {
                    params[i] = sender;
                } else if (paramTypes[i] == String[].class) {
                    params[i] = args;
                } else {
                    params[i] = null; // Placeholder for future parameter injection (e.g., Player)
                }
            }

            method.invoke(bean, params);
        }
    }
}
