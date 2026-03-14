package dev.zyvera_systems.deathpin.util;

import org.bukkit.Location;

public final class DirectionUtil {

    private static final String[] COMPASS = {
            "N", "NNO", "NO", "ONO", "O", "OSO", "SO", "SSO",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    };

    private DirectionUtil() {}

    public static String getDirection(Location from, Location to) {
        double angle = Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX()));
        angle = (angle + 90.0 + 360.0) % 360.0;
        return COMPASS[(int) Math.round(angle / (360.0 / COMPASS.length)) % COMPASS.length];
    }

    public static String coord(double value) {
        return String.valueOf((int) Math.floor(value));
    }
}
