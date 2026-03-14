package dev.zyvera_systems.deathpin.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Only loaded when SchedulerUtil.FOLIA == true. Never reference at class-load time.
final class FoliaScheduler {

    private FoliaScheduler() {}

    static SchedulerUtil.TaskHandle runRepeating(Plugin plugin, Player player, Runnable run, long delay, long period) {
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, t -> run.run(), null, delay, period);
        return task != null ? new FoliaHandle(task) : NoopHandle.INSTANCE;
    }

    static void runDelayed(Plugin plugin, Player player, Runnable run, long delay) {
        player.getScheduler().runDelayed(plugin, t -> run.run(), null, delay);
    }

    static void runSync(Plugin plugin, Runnable run) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> run.run());
    }

    private static final class FoliaHandle implements SchedulerUtil.TaskHandle {
        private final ScheduledTask task;
        FoliaHandle(ScheduledTask task) { this.task = task; }
        @Override public void cancel()         { task.cancel(); }
        @Override public boolean isCancelled() { return task.isCancelled(); }
    }

    private enum NoopHandle implements SchedulerUtil.TaskHandle {
        INSTANCE;
        @Override public void cancel()         {}
        @Override public boolean isCancelled() { return true; }
    }
}
