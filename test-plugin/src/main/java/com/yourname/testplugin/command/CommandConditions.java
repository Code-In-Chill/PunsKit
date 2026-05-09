package com.yourname.testplugin.command;

import com.punshub.punskit.annotation.command.PConditionProvider;
import com.punshub.punskit.annotation.command.arg.PSender;
import com.punshub.punskit.annotation.di.PService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@PService
public class CommandConditions {

    @PConditionProvider("is_op")
    public boolean isOp(@PSender CommandSender sender) {
        return sender.isOp();
    }

    @PConditionProvider("is_flying")
    public boolean isFlying(@PSender Player player) {
        return player.isFlying();
    }
}
