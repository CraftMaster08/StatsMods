package net.craftmaster08.cm08statscore.statstracker;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

/**
 * Proactively resets an individual vanilla stat once it nears Minecraft's
 * int limit, before it can silently wrap around to a negative/garbage value,
 * and records the reset in {@link ConfigManager#intLimits} so leaderboard
 * totals stay correct without any admin intervention.
 */
public class OverflowWatcher {
    private static final Logger LOGGER = LogManager.getLogger(OverflowWatcher.class);

    private final Collection<? extends Stat<?>> watchedStats;
    private final int threshold;
    private final String statLabel;

    public OverflowWatcher(Collection<? extends Stat<?>> watchedStats, int threshold, String statLabel) {
        this.watchedStats = watchedStats;
        this.threshold = threshold;
        this.statLabel = statLabel;
    }

    public void checkPlayer(ServerPlayer player) {
        ConfigManager cfg = StatsCore.getConfigManager();
        if (cfg == null) return;

        for (Stat<?> stat : watchedStats) {
            int value = player.getStats().getValue(stat);
            if (value < threshold) continue;

            player.getStats().setValue(player, stat, 0);
            cfg.intLimits.merge(player.getGameProfile().getName(), 1, Integer::sum);
            cfg.saveConfig();

            player.sendSystemMessage(Component.literal(
                            "[StatsMods] One of your " + statLabel + " stats reached Minecraft's limit and was "
                                    + "automatically reset. Your leaderboard total is unaffected.")
                    .withStyle(ChatFormatting.GOLD));

            LOGGER.info("Auto-reset overflowing {} stat for {} (was {})", statLabel, player.getGameProfile().getName(), value);
        }
    }
}
