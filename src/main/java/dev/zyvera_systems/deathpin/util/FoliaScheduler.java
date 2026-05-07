package dev.zyvera_systems.deathpin.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Only loaded when SchedulerUtil.FOLIA == true. Never reference at class-load time.
final class FoliaScheduler {

    private FoliaScheduler() {}

    static PlatformScheduler.TaskHandle runRepeating(Plugin plugin, Player player, Runnable task, long delay, long period) {
        ScheduledTask t = player.getScheduler().runAtFixedRate(plugin, st -> task.run(), null, delay, period);
        return t != null ? new Handle(t) : NoopHandle.INSTANCE;
    }

    static void runDelayed(Plugin plugin, Player player, Runnable task, long delay) {
        player.getScheduler().runDelayed(plugin, st -> task.run(), null, delay);
    }

    static void runSync(Plugin plugin, Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, st -> task.run());
    }

    private static final class Handle implements PlatformScheduler.TaskHandle {
        private final ScheduledTask task;
        Handle(ScheduledTask task) { this.task = task; }
        @Override public void cancel()         { task.cancel(); }
        @Override public boolean isCancelled() { return task.isCancelled(); }
    }

    private enum NoopHandle implements PlatformScheduler.TaskHandle {
        INSTANCE;
        @Override public void cancel()         {}
        @Override public boolean isCancelled() { return true; }
    }
}
