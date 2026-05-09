package com.punshub.punskit.scheduler;

import com.punshub.punskit.annotation.Scheduled;
import com.punshub.punskit.logging.PunsLogger;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Quản lý việc đăng ký và hủy các tác vụ được lập lịch.
 */
@RequiredArgsConstructor
public class SchedulerManager {

    private final JavaPlugin plugin;
    private final PunsLogger logger;
    private final List<BukkitTask> activeTasks = new ArrayList<>();

    public void registerSchedulers(Collection<Object> beans) {
        int count = 0;
        for (Object bean : beans) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                Scheduled annotation = method.getAnnotation(Scheduled.class);
                if (annotation != null) {
                    scheduleTask(bean, method, annotation);
                    count++;
                }
            }
        }
        if (count > 0) {
            logger.info("✓ Scheduled {} task(s).", count);
        }
    }

    private void scheduleTask(Object bean, Method method, Scheduled annotation) {
        method.setAccessible(true);
        Runnable task = () -> {
            try {
                method.invoke(bean);
            } catch (Exception e) {
                logger.error("Error executing scheduled task: " + method.getName(), e);
            }
        };

        BukkitTask bukkitTask;
        long delay = annotation.delay();
        long period = annotation.period();
        boolean async = annotation.async();
        boolean runOnce = annotation.runOnce() || period <= 0;

        if (runOnce) {
            if (async) {
                bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
            } else {
                bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            if (async) {
                bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
            } else {
                bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
            }
        }

        activeTasks.add(bukkitTask);
        logger.debug("Scheduled task {} in {} (delay={}, period={}, async={})", 
                method.getName(), bean.getClass().getSimpleName(), delay, period, async);
    }

    public void shutdown() {
        int count = 0;
        for (BukkitTask task : activeTasks) {
            if (Bukkit.getScheduler().isQueued(task.getTaskId()) || Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId())) {
                task.cancel();
                count++;
            }
        }
        activeTasks.clear();
        if (count > 0) {
            logger.debug("Cancelled {} scheduled task(s).", count);
        }
    }
}
