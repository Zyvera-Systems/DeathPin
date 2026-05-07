package dev.zyvera_systems.deathpin.trail;

import dev.zyvera_systems.deathpin.DeathPinPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

final class DeathDataStorage {

    private final DeathPinPlugin    plugin;
    private final File              file;
    private       YamlConfiguration yaml;

    DeathDataStorage(DeathPinPlugin plugin) {
        this.plugin = plugin;
        this.file   = new File(plugin.getDataFolder(), "deaths.yml");
        ensureLoaded();
    }

    Map<UUID, Location> loadAll() {
        Map<UUID, Location> result = new HashMap<>();

        if (!yaml.contains("deaths")) return result;

        for (String key : yaml.getConfigurationSection("deaths").getKeys(false)) {
            try {
                UUID  id    = UUID.fromString(key);
                World world = Bukkit.getWorld(yaml.getString("deaths." + key + ".world", ""));
                if (world == null) {
                    plugin.getLogger().warning("World not found for player " + key + " – skipping.");
                    continue;
                }
                result.put(id, new Location(world,
                        yaml.getDouble("deaths." + key + ".x"),
                        yaml.getDouble("deaths." + key + ".y"),
                        yaml.getDouble("deaths." + key + ".z"),
                        (float) yaml.getDouble("deaths." + key + ".yaw"),
                        (float) yaml.getDouble("deaths." + key + ".pitch")
                ));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in deaths.yml: " + key);
            }
        }
        return result;
    }

    void save(UUID id, Location loc) {
        if (loc.getWorld() == null) return;
        String p = "deaths." + id;
        yaml.set(p + ".world", loc.getWorld().getName());
        yaml.set(p + ".x",     loc.getX());
        yaml.set(p + ".y",     loc.getY());
        yaml.set(p + ".z",     loc.getZ());
        yaml.set(p + ".yaw",   (double) loc.getYaw());
        yaml.set(p + ".pitch", (double) loc.getPitch());
        flush();
    }

    private void ensureLoaded() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try { file.createNewFile(); }
            catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Cannot create deaths.yml", e); }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    private void flush() {
        try { yaml.save(file); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Cannot save deaths.yml", e); }
    }
}
