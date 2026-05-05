package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.Service;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@Service
@Slf4j
public class TestListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        log.info("[TestListener] Player joined: {}", event.getPlayer().getName());
    }
}
