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

    private static final String FILE_NAME = "deaths.yml";
    private static final String ROOT      = "deaths";

    private final DeathPinPlugin    plugin;
    private final File              file;
    private       YamlConfiguration yaml;

    DeathDataStorage(DeathPinPlugin plugin) {
        this.plugin = plugin;
        this.file   = new File(plugin.getDataFolder(), FILE_NAME);
    }

    Map<UUID, Location> loadAll() {
        reload();
        Map<UUID, Location> result = new HashMap<>();

        if (!yaml.contains(ROOT)) return result;

        for (String key : yaml.getConfigurationSection(ROOT).getKeys(false)) {
            try {
                UUID   uuid  = UUID.fromString(key);
                String wName = yaml.getString(ROOT + "." + key + ".world");
                World  world = Bukkit.getWorld(wName);
                if (world == null) {
                    plugin.getLogger().warning("World '" + wName + "' not found for " + key + ", skipping.");
                    continue;
                }
                result.put(uuid, new Location(world,
                        yaml.getDouble(ROOT + "." + key + ".x"),
                        yaml.getDouble(ROOT + "." + key + ".y"),
                        yaml.getDouble(ROOT + "." + key + ".z"),
                        (float) yaml.getDouble(ROOT + "." + key + ".yaw"),
                        (float) yaml.getDouble(ROOT + "." + key + ".pitch")
                ));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in deaths.yml: " + key);
            }
        }
        return result;
    }

    void save(UUID id, Location loc) {
        if (loc.getWorld() == null) return;
        String path = ROOT + "." + id;
        yaml.set(path + ".world", loc.getWorld().getName());
        yaml.set(path + ".x",     loc.getX());
        yaml.set(path + ".y",     loc.getY());
        yaml.set(path + ".z",     loc.getZ());
        yaml.set(path + ".yaw",   (double) loc.getYaw());
        yaml.set(path + ".pitch", (double) loc.getPitch());
        flush();
    }

    void remove(UUID id) {
        yaml.set(ROOT + "." + id, null);
        flush();
    }

    private void reload() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create deaths.yml", e);
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    private void flush() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save deaths.yml", e);
        }
    }
}
