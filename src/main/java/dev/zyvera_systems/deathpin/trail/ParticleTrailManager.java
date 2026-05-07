package dev.zyvera_systems.deathpin.trail;

import dev.zyvera_systems.deathpin.DeathPinPlugin;
import dev.zyvera_systems.deathpin.util.PlatformScheduler;
import dev.zyvera_systems.deathpin.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ParticleTrailManager {

    private static final long DEBOUNCE_MS = 500L;

    private final DeathPinPlugin   plugin;
    private final DeathDataStorage storage;

    private final Map<UUID, Location> deaths        = new HashMap<>();
    private final Map<UUID, PlatformScheduler.TaskHandle> activeTasks  = new HashMap<>();
    private final Map<UUID, Long> lastActivated = new HashMap<>();

    public ParticleTrailManager(DeathPinPlugin plugin) {
        this.plugin  = plugin;
        this.storage = new DeathDataStorage(plugin);
        deaths.putAll(storage.loadAll());
        plugin.getLogger().info("Loaded " + deaths.size() + " death location(s).");
    }

    public boolean startTrail(Player player) {
        UUID id  = player.getUniqueId();
        Location loc = deaths.get(id);

        if (loc == null) {
            player.sendMessage(plugin.getConfigManager().msg("trail_no_death"));
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastActivated.getOrDefault(id, 0L) < DEBOUNCE_MS) return false;
        lastActivated.put(id, now);

        if (activeTasks.containsKey(id)) {
            player.sendMessage(plugin.getConfigManager().msg("trail_already_active"));
            return false;
        }

        int period = plugin.getConfigManager().getUpdateTicks();
        activeTasks.put(id, PlatformScheduler.runRepeating(plugin, player, new TrailTask(plugin, id, loc), 1L, period));

        player.sendMessage(plugin.getConfigManager().msg("trail_activated"));
        SoundUtil.play(player, plugin.getConfigManager().getSoundActivate());
        return true;
    }

    public boolean stopTrail(Player player) {
        PlatformScheduler.TaskHandle handle = activeTasks.remove(player.getUniqueId());
        if (handle == null) return false;

        handle.cancel();
        if (player.isOnline()) {
            player.sendMessage(plugin.getConfigManager().msg("trail_deactivated"));
            SoundUtil.play(player, plugin.getConfigManager().getSoundDeactivate());
        }
        return true;
    }

    public void removeTask(UUID id) {
        PlatformScheduler.TaskHandle h = activeTasks.remove(id);
        if (h != null) h.cancel();
    }

    public boolean hasActiveTrail(UUID id) { return activeTasks.containsKey(id); }

    public void setDeathLocation(UUID id, Location loc) {
        deaths.put(id, loc.clone());
        storage.save(id, loc);
    }

    public Location getDeathLocation(UUID id) { return deaths.get(id); }

    public void cancelAll() {
        activeTasks.values().forEach(h -> { try { h.cancel(); } catch (Exception ignored) {} });
        activeTasks.clear();
    }
}
