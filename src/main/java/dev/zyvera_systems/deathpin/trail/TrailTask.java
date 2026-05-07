package dev.zyvera_systems.deathpin.trail;

import dev.zyvera_systems.deathpin.DeathPinPlugin;
import dev.zyvera_systems.deathpin.config.ConfigManager;
import dev.zyvera_systems.deathpin.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

final class TrailTask implements Runnable {

    private static final int    GROUND_SCAN    = 12;
    private static final double GROUND_OFFSET  = 0.15;
    private static final double LOOKAHEAD      = 1.2;
    private static final double MAX_STEER      = Math.toRadians(75);
    private static final double STEER_STEP     = Math.toRadians(22);
    private static final double STEER_DECAY    = 0.55;

    private final DeathPinPlugin plugin;
    private final UUID           playerId;
    private final Location       death;

    private double[] path      = new double[0];
    private int      pathSize  = 0;
    private int      displayAt = 0;

    private int     ticksSinceRecalc  = 0;
    private boolean needsImmediateCalc = true;
    private double  steering = 0.0;
    private boolean finished = false;

    private ParticleBuilder baseBuilder;
    private ParticleBuilder dustBuilder;

    TrailTask(DeathPinPlugin plugin, UUID playerId, Location death) {
        this.plugin   = plugin;
        this.playerId = playerId;
        this.death    = death.clone();
    }

    @Override
    public void run() {
        if (finished) return;

        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) { finish(); return; }

        Location      loc = player.getLocation();
        ConfigManager cfg = plugin.getConfigManager();

        if (!sameWorld(loc)) {
            String world = death.getWorld() != null ? death.getWorld().getName() : "?";
            player.sendMessage(cfg.msg("trail_different_world", "world", world));
            finish();
            return;
        }

        double dist = hDist(loc, death);
        if (dist <= cfg.getArrivalDistance()) {
            player.sendMessage(cfg.msg("trail_arrived"));
            SoundUtil.play(player, cfg.getSoundArrival());
            finish();
            return;
        }

        ticksSinceRecalc += cfg.getUpdateTicks();
        if (needsImmediateCalc || ticksSinceRecalc >= cfg.getRecalculateTicks()) {
            needsImmediateCalc = false;
            ticksSinceRecalc   = 0;
            displayAt          = 0;
            buildPath(loc, dist, cfg);
            buildParticleCache(cfg);
        } else {
            // Advance the visible window to fade the tail each frame
            if (displayAt < pathSize) displayAt++;
        }

        renderPath(player, loc.getWorld(), cfg);
    }

    boolean isFinished() { return finished; }

    private void buildPath(Location start, double totalDist, ConfigManager cfg) {
        World  world    = start.getWorld();
        double spacing  = cfg.getSpacing();
        double startOff = cfg.getStartOffset();
        double maxLen   = Math.min(cfg.getTrailLength(), totalDist - 0.5);

        if (world == null || maxLen <= startOff) { pathSize = 0; return; }

        int capacity = (int) ((maxLen - startOff) / spacing) + 2;
        if (path.length < capacity * 3) path = new double[capacity * 3];
        pathSize = 0;

        double cx = start.getX();
        double cz = start.getZ();
        double cy = start.getY();

        // Skip start-offset without recording points
        double initAngle = Math.atan2(death.getZ() - cz, death.getX() - cx);
        cx += Math.cos(initAngle) * startOff;
        cz += Math.sin(initAngle) * startOff;

        for (double walked = startOff; walked <= maxLen; walked += spacing) {
            double baseAngle = Math.atan2(death.getZ() - cz, death.getX() - cx);

            // Obstacle check and steering update
            double testAngle  = baseAngle + steering;
            double lx = cx + Math.cos(testAngle) * LOOKAHEAD;
            double lz = cz + Math.sin(testAngle) * LOOKAHEAD;
            double lg = groundY(world, lx, lz, cy);

            if (solidWall(world, cx, cy, cz, lx, lg, lz)) {
                boolean leftOk  = !solidWall(world, cx, cy, cz,
                        cx + Math.cos(baseAngle + steering - STEER_STEP) * LOOKAHEAD, cy,
                        cz + Math.sin(baseAngle + steering - STEER_STEP) * LOOKAHEAD);
                boolean rightOk = !solidWall(world, cx, cy, cz,
                        cx + Math.cos(baseAngle + steering + STEER_STEP) * LOOKAHEAD, cy,
                        cz + Math.sin(baseAngle + steering + STEER_STEP) * LOOKAHEAD);

                if      (leftOk && !rightOk) steering = clamp(steering - STEER_STEP);
                else if (rightOk && !leftOk) steering = clamp(steering + STEER_STEP);
                else steering = clamp(steering >= 0 ? steering + STEER_STEP : steering - STEER_STEP);
            } else {
                steering *= STEER_DECAY;
                if (Math.abs(steering) < 0.005) steering = 0.0;
            }

            double angle = baseAngle + steering;
            double nx    = cx + Math.cos(angle) * spacing;
            double nz    = cz + Math.sin(angle) * spacing;
            double ny    = groundY(world, nx, nz, cy) + GROUND_OFFSET;

            int i = pathSize * 3;
            path[i]     = nx;
            path[i + 1] = ny;
            path[i + 2] = nz;
            pathSize++;

            cx = nx; cz = nz; cy = ny - GROUND_OFFSET;
        }
    }

    private void renderPath(Player player, World world, ConfigManager cfg) {
        if (world == null || pathSize == 0 || baseBuilder == null) return;

        ParticleBuilder builder = dustBuilder != null ? dustBuilder : baseBuilder;

        for (int i = displayAt; i < pathSize; i++) {
            builder.location(world, path[i * 3], path[i * 3 + 1], path[i * 3 + 2])
                   .receivers(player)
                   .spawn();
        }
    }

    private void buildParticleCache(ConfigManager cfg) {
        Particle particle = cfg.getTrailParticle();
        baseBuilder = new ParticleBuilder(particle).count(1).offset(0, 0, 0).extra(0);
        if (particle == Particle.DUST) {
            org.bukkit.Color c = cfg.getDustColor();
            dustBuilder = new ParticleBuilder(Particle.DUST)
                    .count(1).offset(0, 0, 0).extra(0)
                    .color(org.bukkit.Color.fromRGB(c.getRed(), c.getGreen(), c.getBlue()), cfg.getDustSize());
        } else {
            dustBuilder = null;
        }
    }

    private static double groundY(World world, double x, double z, double baseY) {
        int bx     = (int) Math.floor(x);
        int bz     = (int) Math.floor(z);
        int startY = (int) Math.floor(baseY) + 2;

        for (int dy = 0; dy <= GROUND_SCAN; dy++) {
            int y = startY - dy;
            if (solid(world.getBlockAt(bx, y, bz))) return y + 1.0;
        }
        return baseY;
    }

    private static boolean solidWall(World w, double x1, double y1, double z1,
                                     double x2, double y2, double z2) {
        double mx = (x1 + x2) * 0.5, mz = (z1 + z2) * 0.5;
        double cy = Math.max(y1, y2);
        int    ix = (int) Math.floor(mx), iz = (int) Math.floor(mz);
        int    dx = (int) Math.floor(x2), dz = (int) Math.floor(z2);

        return solid(w.getBlockAt(ix, (int) Math.floor(cy + 0.5), iz))
            || solid(w.getBlockAt(ix, (int) Math.floor(cy + 1.5), iz))
            || solid(w.getBlockAt(dx, (int) Math.floor(cy + 0.5), dz));
    }

    private static boolean solid(Block b) {
        Material m = b.getType();
        return m.isSolid() && !m.isAir();
    }

    private static double clamp(double v) {
        return Math.max(-MAX_STEER, Math.min(MAX_STEER, v));
    }

    private static double hDist(Location a, Location b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean sameWorld(Location loc) {
        return death.getWorld() != null && loc.getWorld() != null
                && death.getWorld().getUID().equals(loc.getWorld().getUID());
    }

    private void finish() {
        finished = true;
        plugin.getTrailManager().removeTask(playerId);
    }
}
