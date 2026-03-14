package dev.zyvera_systems.deathpin.command;

import dev.zyvera_systems.deathpin.DeathPinPlugin;
import dev.zyvera_systems.deathpin.config.ConfigManager;
import dev.zyvera_systems.deathpin.trail.ParticleTrailManager;
import dev.zyvera_systems.deathpin.util.DirectionUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DeathPinCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS_PLAYER = List.of("show", "hide", "info");
    private static final List<String> SUBS_OP     = List.of("show", "hide", "info", "reload");

    private final DeathPinPlugin plugin;

    public DeathPinCommand(DeathPinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        ConfigManager      cfg = plugin.getConfigManager();
        ParticleTrailManager mgr = plugin.getTrailManager();

        if (args.length == 0) {
            if (!(sender instanceof Player p)) { sender.sendMessage(cfg.msg("cmd_players_only")); return true; }
            if (mgr.hasActiveTrail(p.getUniqueId())) mgr.stopTrail(p);
            else mgr.startTrail(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "show", "start", "on", "enable", "activate" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(cfg.msg("cmd_players_only")); return true; }
                if (!p.hasPermission("deathpin.use")) { p.sendMessage(cfg.msg("cmd_no_permission")); return true; }
                mgr.startTrail(p);
            }
            case "hide", "stop", "off", "disable", "deactivate" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(cfg.msg("cmd_players_only")); return true; }
                if (!p.hasPermission("deathpin.use")) { p.sendMessage(cfg.msg("cmd_no_permission")); return true; }
                if (!mgr.stopTrail(p)) p.sendMessage(cfg.msg("trail_not_active"));
            }
            case "info" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(cfg.msg("cmd_players_only")); return true; }
                sendInfo(p);
            }
            case "reload" -> {
                if (!sender.hasPermission("deathpin.reload")) { sender.sendMessage(cfg.msg("cmd_no_permission")); return true; }
                cfg.reload();
                sender.sendMessage(cfg.msg("config_reloaded"));
            }
            default -> { if (sender instanceof Player p) p.sendMessage(cfg.msg("cmd_usage")); }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> options = sender.hasPermission("deathpin.reload") ? SUBS_OP : SUBS_PLAYER;
        String partial = args[0].toLowerCase();
        return options.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
    }

    private void sendInfo(Player player) {
        ConfigManager      cfg = plugin.getConfigManager();
        ParticleTrailManager mgr = plugin.getTrailManager();
        Location           loc = mgr.getDeathLocation(player.getUniqueId());

        player.sendMessage(cfg.msg("info_header"));

        if (loc == null) {
            player.sendMessage(cfg.msg("info_no_death"));
            return;
        }

        player.sendMessage(cfg.msg("info_location",
                Placeholder.unparsed("x",     DirectionUtil.coord(loc.getX())),
                Placeholder.unparsed("y",     DirectionUtil.coord(loc.getY())),
                Placeholder.unparsed("z",     DirectionUtil.coord(loc.getZ())),
                Placeholder.unparsed("world", loc.getWorld() != null ? loc.getWorld().getName() : "?")
        ));

        player.sendMessage(cfg.msg(mgr.hasActiveTrail(player.getUniqueId())
                ? "info_trail_active" : "info_trail_inactive"));
    }
}
