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

    private static final List<String> PLAYER_SUBS = List.of("show", "hide", "info");
    private static final List<String> OP_SUBS     = List.of("show", "hide", "info", "reload");

    private final DeathPinPlugin plugin;

    public DeathPinCommand(DeathPinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
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
            case "show", "start", "on", "activate" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(cfg.msg("cmd_players_only")); return true; }
                if (!p.hasPermission("deathpin.use"))  { p.sendMessage(cfg.msg("cmd_no_permission")); return true; }
                mgr.startTrail(p);
            }
            case "hide", "stop", "off", "deactivate" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(cfg.msg("cmd_players_only")); return true; }
                if (!p.hasPermission("deathpin.use"))  { p.sendMessage(cfg.msg("cmd_no_permission")); return true; }
                if (!mgr.stopTrail(p)) p.sendMessage(cfg.msg("trail_not_active"));
            }
            case "info" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(cfg.msg("cmd_players_only")); return true; }
                showInfo(p);
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
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> opts = sender.hasPermission("deathpin.reload") ? OP_SUBS : PLAYER_SUBS;
        return opts.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
    }

    private void showInfo(Player player) {
        ConfigManager      cfg = plugin.getConfigManager();
        ParticleTrailManager mgr = plugin.getTrailManager();
        Location           loc = mgr.getDeathLocation(player.getUniqueId());

        player.sendMessage(cfg.msg("info_header"));

        if (loc == null) { player.sendMessage(cfg.msg("info_no_death")); return; }

        player.sendMessage(cfg.msg("info_location",
                Placeholder.unparsed("x",     DirectionUtil.blockCoord(loc.getX())),
                Placeholder.unparsed("y",     DirectionUtil.blockCoord(loc.getY())),
                Placeholder.unparsed("z",     DirectionUtil.blockCoord(loc.getZ())),
                Placeholder.unparsed("world", loc.getWorld() != null ? loc.getWorld().getName() : "?")
        ));

        player.sendMessage(cfg.msg(mgr.hasActiveTrail(player.getUniqueId()) ? "info_trail_active" : "info_trail_inactive"));
    }
}
