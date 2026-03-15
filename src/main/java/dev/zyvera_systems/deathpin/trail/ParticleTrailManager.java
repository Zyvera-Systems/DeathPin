package dev.zyvera_systems.deathpin.trail;

import dev.zyvera_systems.deathpin.DeathPinPlugin;
import dev.zyvera_systems.deathpin.util.SchedulerUtil;
import dev.zyvera_systems.deathpin.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ParticleTrailManager {

    private static final long ACTIVATION_COOLDOWN_MS = 500L;

    private final DeathPinPlugin   plugin;
    private final DeathDataStorage storage;

    private final Map<UUID, Location>                 deathLocations = new HashMap<>();
    private final Map<UUID, SchedulerUtil.TaskHandle> activeTasks    = new HashMap<>();
    private final Map<UUID, Long>                     lastActivation = new HashMap<>();

    public ParticleTrailManager(DeathPinPlugin plugin) {
        this.plugin  = plugin;
        this.storage = new DeathDataStorage(plugin);
        deathLocations.putAll(storage.loadAll());
        plugin.getLogger().info("Loaded " + deathLocations.size() + " death location(s) from disk.");
    }

    public boolean startTrail(Player player) {
        UUID     id  = player.getUniqueId();
        Location loc = deathLocations.get(id);

        if (loc == null) {
            player.sendMessage(plugin.getConfigManager().msg("trail_no_death"));
            return false;
        }

        long now  = System.currentTimeMillis();
        Long last = lastActivation.get(id);
        if (last != null && now - last < ACTIVATION_COOLDOWN_MS) return false;
        lastActivation.put(id, now);

        if (activeTasks.containsKey(id)) {
            player.sendMessage(plugin.getConfigManager().msg("trail_already_active"));
            return false;
        }

        int                    period = plugin.getConfigManager().getUpdateTicks();
        SchedulerUtil.TaskHandle handle = SchedulerUtil.runRepeating(
                plugin, player, new TrailTask(plugin, id, loc), 1L, period);

        activeTasks.put(id, handle);
        player.sendMessage(plugin.getConfigManager().msg("trail_activated"));
        SoundUtil.play(player, plugin.getConfigManager().getSoundActivate());
        return true;
    }

    public boolean stopTrail(Player player) {
        SchedulerUtil.TaskHandle handle = activeTasks.remove(player.getUniqueId());
        if (handle == null) return false;

        handle.cancel();
        if (player.isOnline()) {
            player.sendMessage(plugin.getConfigManager().msg("trail_deactivated"));
            SoundUtil.play(player, plugin.getConfigManager().getSoundDeactivate());
        }
        return true;
    }

    public void removeTask(UUID id) {
        SchedulerUtil.TaskHandle handle = activeTasks.remove(id);
        if (handle != null) handle.cancel();
    }

    public boolean hasActiveTrail(UUID id) {
        return activeTasks.containsKey(id);
    }

    public void setDeathLocation(UUID id, Location location) {
        Location cloned = location.clone();
        deathLocations.put(id, cloned);
        storage.save(id, cloned);
    }

    public Location getDeathLocation(UUID id) {
        return deathLocations.get(id);
    }

    public boolean hasDeathLocation(UUID id) {
        return deathLocations.containsKey(id);
    }

    public void cancelAll() {
        activeTasks.values().forEach(h -> {
            try { h.cancel(); } catch (Exception ignored) { }
        });
        activeTasks.clear();
    }
}
