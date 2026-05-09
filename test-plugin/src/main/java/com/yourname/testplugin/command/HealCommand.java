package com.yourname.testplugin.command;

import com.punshub.punskit.annotation.di.*;
import com.punshub.punskit.annotation.command.*;
import com.punshub.punskit.annotation.command.arg.*;
import com.punshub.punskit.annotation.config.*;
import com.punshub.punskit.annotation.scheduler.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@PService
@PCommand(name = "heal", description = "Heal yourself or others", permission = "punskit.command.heal", aliases = {"h"})
public class HealCommand {

    @PCommandHandler
    @PCooldown(10) // 10s cooldown
    @PCondition(value = "is_op", message = "§cOnly OPs can heal themselves!")
    public void onHeal(@PSender Player player) {
        healPlayer(player);
        player.sendMessage("§aYou have been healed!");
    }

    @PSubcommand("others")
    public void onHealOthers(
            @PSender CommandSender sender,
            @PPlayer(name = "target") Player target,
            @PInt(name = "amount", min = 1, max = 20, optional = true, defaultValue = 20) int amount
    ) {
        healPlayer(target);
        sender.sendMessage("§eHealed " + target.getName() + " for " + amount + " HP (Simulated)!");
    }

    private void healPlayer(Player player) {
        var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = attribute != null ? attribute.getValue() : 20.0;
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
    }
}
