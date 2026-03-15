package dev.zyvera_systems.deathpin.trail;

import dev.zyvera_systems.deathpin.DeathPinPlugin;
import dev.zyvera_systems.deathpin.config.ConfigManager;
import dev.zyvera_systems.deathpin.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class TrailTask implements Runnable {

    private static final int    GROUND_SCAN_DEPTH  = 12;
    private static final double GROUND_Y_OFFSET    = 0.15;
    private static final double OBSTACLE_LOOKAHEAD = 1.2;
    private static final double MAX_STEERING       = Math.toRadians(75);
    private static final double STEER_STEP         = Math.toRadians(22);
    private static final double STEER_DECAY        = 0.55;

    private final DeathPinPlugin plugin;
    private final UUID           playerId;
    private final Location       deathLocation;

    private List<Location> cachedPath      = Collections.emptyList();
    private int            displayStart    = 0;
    private int            ticksSinceRecalc = 0;
    private boolean        needsImmediateCalc = true;
    private double         steering        = 0.0;
    private boolean        finished        = false;

    TrailTask(DeathPinPlugin plugin, UUID playerId, Location deathLocation) {
        this.plugin        = plugin;
        this.playerId      = playerId;
        this.deathLocation = deathLocation.clone();
    }

    @Override
    public void run() {
        if (finished) return;

        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            finish();
            return;
        }

        Location    playerLoc = player.getLocation();
        ConfigManager cfg     = plugin.getConfigManager();

        if (!sameWorld(playerLoc)) {
            String world = deathLocation.getWorld() != null ? deathLocation.getWorld().getName() : "?";
            player.sendMessage(cfg.msg("trail_different_world", "world", world));
            finish();
            return;
        }

        double distance = horizontalDistance(playerLoc, deathLocation);
        if (distance <= cfg.getArrivalDistance()) {
            player.sendMessage(cfg.msg("trail_arrived"));
            SoundUtil.play(player, cfg.getSoundArrival());
            finish();
            return;
        }

        ticksSinceRecalc += cfg.getUpdateTicks();
        if (needsImmediateCalc || ticksSinceRecalc >= cfg.getRecalculateTicks()) {
            needsImmediateCalc = false;
            ticksSinceRecalc   = 0;
            displayStart       = 0;
            cachedPath         = computePath(playerLoc, distance, cfg);
        } else {
            // Trim one point from the tail each frame so the back fades naturally
            displayStart = Math.min(displayStart + 1, cachedPath.size());
        }

        World    world    = playerLoc.getWorld();
        Particle particle = cfg.getTrailParticle();

        for (int i = displayStart; i < cachedPath.size(); i++) {
            Location p = cachedPath.get(i);
            spawnParticle(player, particle, world, p.getX(), p.getY(), p.getZ(), cfg);
        }
    }

    boolean isFinished() {
        return finished;
    }

    private List<Location> computePath(Location start, double totalDist, ConfigManager cfg) {
        World  world    = start.getWorld();
        double spacing  = cfg.getSpacing();
        double startOff = cfg.getStartOffset();
        double maxLen   = Math.min(cfg.getTrailLength(), totalDist - 0.5);

        if (maxLen <= startOff || world == null) return Collections.emptyList();

        List<Location> path = new ArrayList<>((int) ((maxLen - startOff) / spacing) + 1);

        double cx = start.getX();
        double cz = start.getZ();
        double cy = start.getY();

        double initAngle = Math.atan2(deathLocation.getZ() - cz, deathLocation.getX() - cx);
        cx += Math.cos(initAngle) * startOff;
        cz += Math.sin(initAngle) * startOff;

        double walked = startOff;

        while (walked <= maxLen) {
            double baseAngle = Math.atan2(deathLocation.getZ() - cz, deathLocation.getX() - cx);
            double testAngle = baseAngle + steering;

            double lookX      = cx + Math.cos(testAngle) * OBSTACLE_LOOKAHEAD;
            double lookZ      = cz + Math.sin(testAngle) * OBSTACLE_LOOKAHEAD;
            double groundLook = groundHeight(world, lookX, lookZ, cy);
            boolean blocked   = isSolidWall(world, cx, cy, cz, lookX, groundLook, lookZ);

            if (blocked) {
                double leftAngle  = baseAngle + steering - STEER_STEP;
                double rightAngle = baseAngle + steering + STEER_STEP;
                boolean leftClear  = !isSolidWall(world, cx, cy, cz,
                        cx + Math.cos(leftAngle)  * OBSTACLE_LOOKAHEAD, cy,
                        cz + Math.sin(leftAngle)  * OBSTACLE_LOOKAHEAD);
                boolean rightClear = !isSolidWall(world, cx, cy, cz,
                        cx + Math.cos(rightAngle) * OBSTACLE_LOOKAHEAD, cy,
                        cz + Math.sin(rightAngle) * OBSTACLE_LOOKAHEAD);

                if (leftClear && !rightClear) {
                    steering = clamp(steering - STEER_STEP);
                } else if (rightClear && !leftClear) {
                    steering = clamp(steering + STEER_STEP);
                } else {
                    steering = clamp(steering >= 0 ? steering + STEER_STEP : steering - STEER_STEP);
                }
            } else {
                steering *= STEER_DECAY;
                if (Math.abs(steering) < 0.005) steering = 0.0;
            }

            double moveAngle = baseAngle + steering;
            double nx        = cx + Math.cos(moveAngle) * spacing;
            double nz        = cz + Math.sin(moveAngle) * spacing;
            double groundY   = groundHeight(world, nx, nz, cy);

            path.add(new Location(world, nx, groundY + GROUND_Y_OFFSET, nz));

            cx = nx;
            cz = nz;
            cy = groundY;
            walked += spacing;
        }

        return path;
    }

    private static double groundHeight(World world, double x, double z, double baseY) {
        int bx     = (int) Math.floor(x);
        int bz     = (int) Math.floor(z);
        int startY = (int) Math.floor(baseY) + 2;

        for (int dy = 0; dy <= GROUND_SCAN_DEPTH; dy++) {
            int checkY = startY - dy;
            if (isSolid(world.getBlockAt(bx, checkY, bz))) {
                return checkY + 1.0;
            }
        }
        return baseY;
    }

    private static boolean isSolidWall(World world,
                                       double x1, double y1, double z1,
                                       double x2, double y2, double z2) {
        double mx     = (x1 + x2) * 0.5;
        double mz     = (z1 + z2) * 0.5;
        double checkY = Math.max(y1, y2);

        return isSolid(world.getBlockAt((int) Math.floor(mx), (int) Math.floor(checkY + 0.5), (int) Math.floor(mz)))
            || isSolid(world.getBlockAt((int) Math.floor(mx), (int) Math.floor(checkY + 1.5), (int) Math.floor(mz)))
            || isSolid(world.getBlockAt((int) Math.floor(x2),  (int) Math.floor(checkY + 0.5), (int) Math.floor(z2)));
    }

    private static boolean isSolid(Block block) {
        if (block == null) return false;
        Material m = block.getType();
        return m.isSolid() && !m.isAir();
    }

    @SuppressWarnings("deprecation")
    private static void spawnParticle(Player player, Particle particle, World world,
                                      double x, double y, double z, ConfigManager cfg) {
        if (world == null) return;

        if (particle == Particle.CLOUD|| particle.name().equals("REDSTONE")) {
            org.bukkit.Color col = cfg.getDustColor();
            Particle.DustOptions dust = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(col.getRed(), col.getGreen(), col.getBlue()),
                    cfg.getDustSize()
            );
            player.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0, dust);
        } else {
            player.spawnParticle(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static double clamp(double s) {
        return Math.max(-MAX_STEERING, Math.min(MAX_STEERING, s));
    }

    private void finish() {
        finished = true;
        plugin.getTrailManager().removeTask(playerId);
    }

    private boolean sameWorld(Location loc) {
        return deathLocation.getWorld() != null
                && loc.getWorld() != null
                && deathLocation.getWorld().getUID().equals(loc.getWorld().getUID());
    }

    private static double horizontalDistance(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
