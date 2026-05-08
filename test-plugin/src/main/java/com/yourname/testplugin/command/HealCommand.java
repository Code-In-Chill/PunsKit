package com.yourname.testplugin.command;

import com.punshub.punskit.annotation.Command;
import com.punshub.punskit.annotation.CommandHandler;
import com.punshub.punskit.annotation.Service;
import com.punshub.punskit.annotation.Subcommand;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Service
@Command(name = "heal", description = "Heal yourself or others", permission = "punskit.command.heal", aliases = {"h"})
public class HealCommand {

    @CommandHandler
    public void onHeal(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can heal themselves.");
            return;
        }
        healPlayer(player);
        player.sendMessage("§aYou have been healed!");
    }

    @Subcommand("others")
    public void onHealOthers(CommandSender sender, String[] args) {
        sender.sendMessage("§eHeal others subcommand reached! Args: " + String.join(", ", args));
    }

    private void healPlayer(Player player) {
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
    }
}
