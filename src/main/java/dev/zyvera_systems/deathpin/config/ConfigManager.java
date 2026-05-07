package dev.zyvera_systems.deathpin.config;

import dev.zyvera_systems.deathpin.DeathPinPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;

public final class ConfigManager {

    private static final Set<String> NO_PREFIX = Set.of(
            "death_header", "death_click_button", "death_click_hover", "death_click_command", "info_header"
    );

    private final DeathPinPlugin plugin;
    private final MiniMessage    mm = MiniMessage.miniMessage();

    private boolean  showCoordinates, showDirection, showWorld;
    private boolean  trailEnabled;
    private Particle trailParticle;
    private Color    dustColor;
    private float    dustSize;
    private double   trailLength, spacing, startOffset, arrivalDistance;
    private int      updateTicks, recalculateTicks;
    private String   soundActivate, soundDeactivate, soundArrival;
    private String   prefix;

    public ConfigManager(DeathPinPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        showCoordinates  = c.getBoolean("message.show_coordinates", true);
        showDirection    = c.getBoolean("message.show_direction",   true);
        showWorld        = c.getBoolean("message.show_world",       false);

        trailEnabled     = c.getBoolean("trail.enabled",             true);
        trailParticle    = parseParticle(c.getString("trail.particle", "END_ROD"));
        dustColor        = parseDustColor(c.getString("trail.dust_color", "255,80,80"));
        dustSize         = (float) c.getDouble("trail.dust_size",    1.2);
        trailLength      = c.getDouble("trail.trail_length",         9.0);
        spacing          = c.getDouble("trail.spacing",              0.5);
        startOffset      = c.getDouble("trail.start_offset",         1.8);
        updateTicks      = Math.max(1, c.getInt("trail.update_ticks",       3));
        recalculateTicks = Math.max(updateTicks, c.getInt("trail.recalculate_ticks", 30));
        arrivalDistance  = c.getDouble("trail.arrival_distance",     2.5);

        soundActivate    = c.getString("sound.on_activate",   "ENTITY_EXPERIENCE_ORB_PICKUP");
        soundDeactivate  = c.getString("sound.on_deactivate", "BLOCK_NOTE_BLOCK_BASS");
        soundArrival     = c.getString("sound.on_arrival",    "ENTITY_PLAYER_LEVELUP");

        prefix = c.getString("messages.prefix",
                "<dark_gray>[<red>✝</red> <white>DeathPin</white><dark_gray>] ");
    }

    public Component msg(String key, TagResolver... resolvers) {
        String raw = plugin.getConfig().getString("messages." + key, "<red>[DeathPin] Missing: " + key);
        if (!NO_PREFIX.contains(key)) raw = prefix + raw;
        return mm.deserialize(raw, resolvers);
    }

    public Component msg(String key, String tag, String value) {
        return msg(key, Placeholder.unparsed(tag, value));
    }

    public String rawMsg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    public boolean  isShowCoordinates()   { return showCoordinates; }
    public boolean  isShowDirection()     { return showDirection; }
    public boolean  isShowWorld()         { return showWorld; }
    public boolean  isTrailEnabled()      { return trailEnabled; }
    public Particle getTrailParticle()    { return trailParticle; }
    public Color    getDustColor()        { return dustColor; }
    public float    getDustSize()         { return dustSize; }
    public double   getTrailLength()      { return trailLength; }
    public double   getSpacing()          { return spacing; }
    public double   getStartOffset()      { return startOffset; }
    public int      getUpdateTicks()      { return updateTicks; }
    public int      getRecalculateTicks() { return recalculateTicks; }
    public double   getArrivalDistance()  { return arrivalDistance; }
    public String   getSoundActivate()    { return soundActivate; }
    public String   getSoundDeactivate()  { return soundDeactivate; }
    public String   getSoundArrival()     { return soundArrival; }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown particle '" + name + "', using END_ROD.");
            return Particle.END_ROD;
        }
    }

    private Color parseDustColor(String raw) {
        try {
            String[] p = raw.split(",");
            return Color.fromRGB(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid dust_color '" + raw + "', using red.");
            return Color.fromRGB(255, 80, 80);
        }
    }
}
