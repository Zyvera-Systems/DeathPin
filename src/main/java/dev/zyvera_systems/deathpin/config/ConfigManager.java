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

    private boolean  showCoordinates;
    private boolean  showDirection;
    private boolean  showWorld;
    private boolean  trailEnabled;
    private Particle trailParticle;
    private Color    dustColor;
    private float    dustSize;
    private double   trailLength;
    private double   spacing;
    private double   startOffset;
    private int      updateTicks;
    private int      recalculateTicks;
    private double   arrivalDistance;
    private String   soundActivate;
    private String   soundDeactivate;
    private String   soundArrival;
    private String   prefix;

    public ConfigManager(DeathPinPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        showCoordinates  = cfg.getBoolean("message.show_coordinates", true);
        showDirection    = cfg.getBoolean("message.show_direction",   true);
        showWorld        = cfg.getBoolean("message.show_world",       false);

        trailEnabled     = cfg.getBoolean("trail.enabled",              true);
        trailParticle    = parseParticle(cfg.getString("trail.particle", "END_ROD"));
        dustColor        = parseDustColor(cfg.getString("trail.dust_color", "255,80,80"));
        dustSize         = (float) cfg.getDouble("trail.dust_size",     1.2);
        trailLength      = cfg.getDouble("trail.trail_length",          9.0);
        spacing          = cfg.getDouble("trail.spacing",               0.5);
        startOffset      = cfg.getDouble("trail.start_offset",          1.8);
        updateTicks      = Math.max(1, cfg.getInt("trail.update_ticks",       3));
        recalculateTicks = Math.max(updateTicks, cfg.getInt("trail.recalculate_ticks", 30));
        arrivalDistance  = cfg.getDouble("trail.arrival_distance",      2.5);

        soundActivate    = cfg.getString("sound.on_activate",   "ENTITY_EXPERIENCE_ORB_PICKUP");
        soundDeactivate  = cfg.getString("sound.on_deactivate", "BLOCK_NOTE_BLOCK_BASS");
        soundArrival     = cfg.getString("sound.on_arrival",    "ENTITY_PLAYER_LEVELUP");

        prefix = cfg.getString("messages.prefix",
                "<dark_gray>[<red>✝</red> <white>DeathPin</white><dark_gray>] ");
    }

    public Component msg(String key, TagResolver... resolvers) {
        FileConfiguration cfg = plugin.getConfig();
        String raw = cfg.getString("messages." + key, "<red>[DeathPin] Missing: " + key);
        if (!NO_PREFIX.contains(key)) raw = prefix + raw;
        return mm.deserialize(raw, resolvers);
    }

    public Component msg(String key, String placeholder, String value) {
        return msg(key, Placeholder.unparsed(placeholder, value));
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
            plugin.getLogger().warning("Unknown particle '" + name + "', falling back to END_ROD.");
            return Particle.END_ROD;
        }
    }

    private Color parseDustColor(String raw) {
        try {
            String[] parts = raw.split(",");
            return Color.fromRGB(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid dust_color '" + raw + "', using red.");
            return Color.fromRGB(255, 80, 80);
        }
    }
}
