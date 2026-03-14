package dev.zyvera_systems.deathpin.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Scheduler bridge for Bukkit/Spigot/Paper and Folia.
 * Folia-specific code is isolated in {@link FoliaScheduler} so the JVM
 * never loads it on non-Folia servers, preventing NoClassDefFoundError.
 */
public final class SchedulerUtil {

    static final boolean FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) { }
        FOLIA = folia;
    }

    private SchedulerUtil() {}

    public static boolean isFolia() { return FOLIA; }

    public static TaskHandle runRepeating(Plugin plugin, Player player, Runnable run, long delay, long period) {
        if (FOLIA) return FoliaScheduler.runRepeating(plugin, player, run, delay, period);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, run, delay, period);
        return new BukkitHandle(task);
    }

    public static void runDelayed(Plugin plugin, Player player, Runnable run, long delay) {
        if (FOLIA) FoliaScheduler.runDelayed(plugin, player, run, delay);
        else Bukkit.getScheduler().runTaskLater(plugin, run, delay);
    }

    public static void runSync(Plugin plugin, Runnable run) {
        if (FOLIA) FoliaScheduler.runSync(plugin, run);
        else Bukkit.getScheduler().runTask(plugin, run);
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
