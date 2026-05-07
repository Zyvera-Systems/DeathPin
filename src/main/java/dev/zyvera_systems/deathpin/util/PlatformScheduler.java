package dev.zyvera_systems.deathpin.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class PlatformScheduler {

    static final boolean FOLIA;

    static {
        boolean detected = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            detected = true;
        } catch (ClassNotFoundException ignored) {}
        FOLIA = detected;
    }

    private PlatformScheduler() {}

    public static boolean isFolia() { return FOLIA; }

    public static TaskHandle runRepeating(Plugin plugin, Player player, Runnable task, long delay, long period) {
        if (FOLIA) return FoliaScheduler.runRepeating(plugin, player, task, delay, period);
        return new BukkitHandle(Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period));
    }

    public static void runDelayed(Plugin plugin, Player player, Runnable task, long delay) {
        if (FOLIA) FoliaScheduler.runDelayed(plugin, player, task, delay);
        else Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    public interface TaskHandle {
        void cancel();
        boolean isCancelled();
    }

    private record BukkitHandle(BukkitTask task) implements TaskHandle {
        @Override public void cancel()         { task.cancel(); }
        @Override public boolean isCancelled() { return task.isCancelled(); }
    }
}
