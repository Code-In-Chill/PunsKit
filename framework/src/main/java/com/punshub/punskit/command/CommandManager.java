package com.punshub.punskit.command;

import com.punshub.punskit.annotation.di.*;
import com.punshub.punskit.annotation.command.*;
import com.punshub.punskit.annotation.command.arg.*;
import com.punshub.punskit.annotation.config.*;
import com.punshub.punskit.annotation.scheduler.*;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý đăng ký và điều hướng lệnh nâng cao.
 */
@RequiredArgsConstructor
public class CommandManager {

    private final JavaPlugin plugin;
    private final ConditionRegistry conditionRegistry;
    private final PunsLogger logger;
    private CommandMap commandMap;

    // Track registered commands for cleanup
    private final List<String> registeredCommands = new ArrayList<>();

    // Cooldown storage: CommandKey -> UUID -> Timestamp
    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    public void registerCommands(Collection<Object> beans) {
        if (commandMap == null) {
            initializeCommandMap();
        }

        if (commandMap == null) {
            logger.error("Skipping command registration: CommandMap could not be initialized.");
            return;
        }

        int count = 0;
        for (Object bean : beans) {
            PCommand annotation = bean.getClass().getAnnotation(PCommand.class);
            if (annotation != null) {
                registerCommand(bean, annotation);
                count++;
            }
        }

        if (count > 0) {
            logger.info("✓ Auto-registered {} advanced command(s).", count);
        }
    }

    public void cleanup() {
        cooldowns.clear();
        logger.debug("Cleared command cooldowns.");

        if (commandMap != null) {
            unregisterCommands();
        }
    }

    private void initializeCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            this.commandMap = (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            throw new com.punshub.punskit.exception.FrameworkException("Failed to access Bukkit CommandMap. Advanced commands will not work.", e);
        }
    }

    private void registerCommand(Object bean, PCommand annotation) {
        String name = annotation.name();
        PunsWrappedCommand wrappedCommand = new PunsWrappedCommand(name, bean, annotation, plugin, conditionRegistry, logger, cooldowns);
        
        commandMap.register(plugin.getName().toLowerCase(), wrappedCommand);
        registeredCommands.add(name.toLowerCase());
        for (String alias : annotation.aliases()) {
            registeredCommands.add(alias.toLowerCase());
        }
        logger.debug("Registered advanced command: /{}", name);
    }

    private void unregisterCommands() {
        try {
            Field knownCommandsField = org.bukkit.command.SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            int count = 0;
            for (String label : registeredCommands) {
                String pluginPrefix = plugin.getName().toLowerCase() + ":" + label;
                if (knownCommands.containsKey(label)) {
                    org.bukkit.command.Command cmd = knownCommands.get(label);
                    if (cmd instanceof PunsWrappedCommand) {
                        cmd.unregister(commandMap);
                        knownCommands.remove(label);
                        knownCommands.remove(pluginPrefix);
                        count++;
                    }
                }
            }
            registeredCommands.clear();
            if (count > 0) {
                logger.debug("Unregistered {} advanced command(s).", count);
            }
        } catch (Exception e) {
            logger.error("Failed to unregister commands during cleanup", e);
        }
    }

    public void execute(Object bean, PCommand annotation, CommandSender sender, String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        String[] parts = trimmed.split("\\s+");
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
        new PunsWrappedCommand(annotation.name(), bean, annotation, plugin, conditionRegistry, logger, cooldowns).execute(sender, annotation.name(), args);
    }

    private static class PunsWrappedCommand extends BukkitCommand {
        private final Object bean;
        private final JavaPlugin plugin;
        private final ConditionRegistry conditionRegistry;
        private final PunsLogger logger;
        private final Map<String, Map<UUID, Long>> cooldowns;
        private Method mainHandler;
        private final Map<String, Method> subcommands = new HashMap<>();
        
        protected PunsWrappedCommand(String name, Object bean, PCommand annotation, JavaPlugin plugin, ConditionRegistry conditionRegistry, PunsLogger logger, Map<String, Map<UUID, Long>> cooldowns) {
            super(name);
            this.bean = bean;
            this.plugin = plugin;
            this.conditionRegistry = conditionRegistry;
            this.logger = logger.withContext("Command:" + name);
            this.cooldowns = cooldowns;
            
            setLabel(name);
            setDescription(annotation.description());
            setPermission(annotation.permission().isEmpty() ? null : annotation.permission());
            setAliases(Arrays.asList(annotation.aliases()));

            parseHandlers();
        }

        private void parseHandlers() {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(PCommandHandler.class)) {
                    this.mainHandler = method;
                    this.mainHandler.setAccessible(true);
                } else if (method.isAnnotationPresent(PSubcommand.class)) {
                    PSubcommand sub = method.getAnnotation(PSubcommand.class);
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
                Method targetMethod;
                String[] effectiveArgs;

                if (args.length > 0 && subcommands.containsKey(args[0].toLowerCase())) {
                    targetMethod = subcommands.get(args[0].toLowerCase());
                    effectiveArgs = Arrays.copyOfRange(args, 1, args.length);
                } else {
                    targetMethod = mainHandler;
                    effectiveArgs = args;
                }

                if (targetMethod == null) {
                    sendUsage(sender);
                    return true;
                }

                // Check Conditions
                if (targetMethod.isAnnotationPresent(PCondition.class)) {
                    PCondition cond = targetMethod.getAnnotation(PCondition.class);
                    if (!conditionRegistry.check(cond.value(), sender)) {
                        sender.sendMessage(cond.message());
                        return true;
                    }
                }

                // Check Cooldown
                if (sender instanceof Player player && targetMethod.isAnnotationPresent(PCooldown.class)) {
                    if (checkCooldown(player, targetMethod)) return true;
                }

                // Execute Async or Sync
                if (targetMethod.isAnnotationPresent(PAsync.class)) {
                    PAsync asyncAnno = targetMethod.getAnnotation(PAsync.class);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        invokeWithParsing(targetMethod, sender, effectiveArgs);
                        if (asyncAnno.syncOnComplete()) {
                            Bukkit.getScheduler().runTask(plugin, () -> {}); // Dummy task to sync, or we could wrap invokeWithParsing
                        }
                    });
                } else {
                    invokeWithParsing(targetMethod, sender, effectiveArgs);
                }

            } catch (Exception e) {
                logger.error("Error executing command /" + getName(), e);
                sender.sendMessage("§cAn internal error occurred.");
            }
            return true;
        }

        private boolean checkCooldown(Player player, Method method) {
            PCooldown cooldownAnno = method.getAnnotation(PCooldown.class);
            String key = getName() + ":" + method.getName();
            Map<UUID, Long> methodCooldowns = cooldowns.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
            
            long now = System.currentTimeMillis();
            long lastUsed = methodCooldowns.getOrDefault(player.getUniqueId(), 0L);
            long diff = (now - lastUsed) / 1000;

            if (diff < cooldownAnno.value()) {
                player.sendMessage("§cPlease wait " + (cooldownAnno.value() - diff) + "s before using this again.");
                return true;
            }
            
            methodCooldowns.put(player.getUniqueId(), now);
            return false;
        }

        private void invokeWithParsing(Method method, CommandSender sender, String[] args) {
            try {
                Parameter[] params = method.getParameters();
                Object[] values = new Object[params.length];
                int argIndex = 0;

                for (int i = 0; i < params.length; i++) {
                    Parameter p = params[i];
                    
                    if (p.isAnnotationPresent(PSender.class)) {
                        values[i] = resolveSender(p.getType(), sender);
                        if (values[i] == null) {
                            sender.sendMessage("§cOnly " + p.getType().getSimpleName() + " can use this command.");
                            return;
                        }
                    } else if (p.isAnnotationPresent(PInt.class)) {
                        PInt anno = p.getAnnotation(PInt.class);
                        String raw = getArg(args, argIndex, anno.optional() ? String.valueOf(anno.defaultValue()) : null);
                        if (raw == null) { sendMissingArg(sender, anno.name()); return; }
                        if (argIndex < args.length) argIndex++;
                        
                        try {
                            int val = Integer.parseInt(raw);
                            if (val < anno.min() || val > anno.max()) {
                                sender.sendMessage("§c" + anno.name() + " must be between " + anno.min() + " and " + anno.max());
                                return;
                            }
                            values[i] = val;
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§c" + anno.name() + " must be a number.");
                            return;
                        }
                    } else if (p.isAnnotationPresent(PText.class)) {
                        PText anno = p.getAnnotation(PText.class);
                        String val = getArg(args, argIndex, anno.optional() ? anno.defaultValue() : null);
                        if (val == null) { sendMissingArg(sender, anno.name()); return; }
                        if (argIndex < args.length) argIndex++;
                        values[i] = val;
                    } else if (p.isAnnotationPresent(PPlayer.class)) {
                        PPlayer anno = p.getAnnotation(PPlayer.class);
                        String raw = getArg(args, argIndex, null);
                        if (raw == null) {
                            if (anno.optional()) values[i] = null;
                            else { sendMissingArg(sender, anno.name()); return; }
                        } else {
                            if (argIndex < args.length) argIndex++;
                            Player target = Bukkit.getPlayer(raw);
                            if (target == null) { sender.sendMessage("§cPlayer not found: " + raw); return; }
                            values[i] = target;
                        }
                    } else {
                        // Default fallback for legacy/unannotated parameters
                        if (p.getType().isAssignableFrom(sender.getClass())) values[i] = sender;
                        else if (p.getType() == String[].class) values[i] = args;
                        else values[i] = null;
                    }
                }

                method.invoke(bean, values);
            } catch (Exception e) {
                logger.error("Failed to invoke command handler", e);
                sender.sendMessage("§cAn error occurred during command execution.");
            }
        }

        private Object resolveSender(Class<?> type, CommandSender sender) {
            if (type.isAssignableFrom(sender.getClass())) return sender;
            return null;
        }

        private String getArg(String[] args, int index, String def) {
            if (index < args.length) return args[index];
            return def;
        }

        private void sendMissingArg(CommandSender sender, String name) {
            sender.sendMessage("§cMissing required argument: <" + name + ">");
        }

        private void sendUsage(CommandSender sender) {
            sender.sendMessage("§cUsage: /" + getName() + " [" + String.join("|", subcommands.keySet()) + "]");
        }

        @Override
        public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>(subcommands.keySet());
                return completions.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
            }
            return Collections.emptyList();
        }
    }
}
