package dev.zyvera_systems.deathpin.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {

    private SoundUtil() {}

    public static void play(Player player, String name) {
        if (name == null || name.isBlank()) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name.toUpperCase()), 0.8f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }
}
