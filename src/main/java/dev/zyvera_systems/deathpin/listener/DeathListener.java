package dev.zyvera_systems.deathpin.listener;

import dev.zyvera_systems.deathpin.DeathPinPlugin;
import dev.zyvera_systems.deathpin.config.ConfigManager;
import dev.zyvera_systems.deathpin.util.DirectionUtil;
import dev.zyvera_systems.deathpin.util.PlatformScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class DeathListener implements Listener {

    private final DeathPinPlugin plugin;
    private final MiniMessage    mm = MiniMessage.miniMessage();

    public DeathListener(DeathPinPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getTrailManager().setDeathLocation(player.getUniqueId(), player.getLocation().clone());
        plugin.getTrailManager().stopTrail(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlatformScheduler.runDelayed(plugin, player, () -> {
            if (player.isOnline()) sendDeathMessage(player);
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTrailManager().stopTrail(event.getPlayer());
    }

    private void sendDeathMessage(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        Location      loc = plugin.getTrailManager().getDeathLocation(player.getUniqueId());

        if (loc == null) return;

        player.sendMessage(Component.empty());
        player.sendMessage(cfg.msg("death_header"));

        if (cfg.isShowCoordinates()) {
            player.sendMessage(cfg.msg("death_location",
                    Placeholder.unparsed("x", DirectionUtil.blockCoord(loc.getX())),
                    Placeholder.unparsed("y", DirectionUtil.blockCoord(loc.getY())),
                    Placeholder.unparsed("z", DirectionUtil.blockCoord(loc.getZ()))
            ));
        }

        if (cfg.isShowDirection()) {
            player.sendMessage(cfg.msg("death_direction",
                    Placeholder.unparsed("direction", DirectionUtil.bearing(player.getLocation(), loc))
            ));
        }

        if (cfg.isShowWorld() && loc.getWorld() != null) {
            player.sendMessage(cfg.msg("death_world", "world", loc.getWorld().getName()));
        }

        if (cfg.isTrailEnabled()) {
            Component button = cfg.msg("death_click_button")
                    .clickEvent(ClickEvent.runCommand(cfg.rawMsg("death_click_command")))
                    .hoverEvent(HoverEvent.showText(mm.deserialize(cfg.rawMsg("death_click_hover"))));
            player.sendMessage(Component.empty());
            player.sendMessage(button);
        }

        player.sendMessage(Component.empty());
    }
}
