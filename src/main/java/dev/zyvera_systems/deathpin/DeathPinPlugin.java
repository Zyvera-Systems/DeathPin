package dev.zyvera_systems.deathpin;

import dev.zyvera_systems.deathpin.command.DeathPinCommand;
import dev.zyvera_systems.deathpin.config.ConfigManager;
import dev.zyvera_systems.deathpin.listener.DeathListener;
import dev.zyvera_systems.deathpin.trail.ParticleTrailManager;
import dev.zyvera_systems.deathpin.util.SchedulerUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DeathPinPlugin extends JavaPlugin {

    private static DeathPinPlugin instance;

    private ConfigManager        configManager;
    private ParticleTrailManager trailManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        configManager = new ConfigManager(this);
        trailManager  = new ParticleTrailManager(this);

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        DeathPinCommand executor = new DeathPinCommand(this);
        PluginCommand   cmd      = getCommand("deathpin");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().severe("Could not register /deathpin – check plugin.yml!");
        }

        getLogger().info("DeathPin v" + getDescription().getVersion()
                + " enabled (Folia: " + SchedulerUtil.isFolia() + ")");
    }

    @Override
    public void onDisable() {
        if (trailManager != null) trailManager.cancelAll();
    }

    public static DeathPinPlugin getInstance() { return instance; }
    public ConfigManager         getConfigManager() { return configManager; }
    public ParticleTrailManager  getTrailManager()  { return trailManager; }
}
