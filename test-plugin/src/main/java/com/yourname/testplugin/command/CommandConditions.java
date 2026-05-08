package com.yourname.testplugin.command;

import com.punshub.punskit.annotation.PConditionProvider;
import com.punshub.punskit.annotation.PSender;
import com.punshub.punskit.annotation.Service;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Service
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
