package net.craftmaster08.distanceleaderboard;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.ranking.LeaderboardFormatter;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class LeaderboardExecutor {
    private final CommandSourceStack source;
    private final MinecraftServer server;
    private final ConfigManager config;
    private final DailyStatsTracker dailyStatsTracker;
    private final StatsTracker statsTracker;
    private final Logger LOGGER;

    public LeaderboardExecutor(CommandSourceStack source, Logger LOGGER) {
        this.source = source;
        this.server = DistanceLeaderboard.getServer();
        this.config = StatsCore.getConfigManager();
        this.LOGGER = LOGGER;
        this.statsTracker = DistanceLeaderboard.getStatsTracker();
        this.dailyStatsTracker = DistanceLeaderboard.getDailyStatsTracker();
    }

    int execute() {
        if (server == null) {
            sendError("Server not initialized");
            return 0;
        }
        if (config == null) {
            sendError("StatsCore configuration not initialized");
            return 0;
        }
        if (statsTracker == null) {
            sendError("StatsTracker not initialized");
            return 0;
        }
        if (dailyStatsTracker == null) {
            LOGGER.warn("DailyStatsTracker unavailable; daily distance hover text disabled");
        }

        try {
            if (!StatsCore.canUseLeaderboard(source.getPlayerOrException().getUUID())) {
                source.sendSystemMessage(Component.literal("Please wait " + config.cooldownSeconds + "s before using this command again.")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            LOGGER.error("No player found {}", e.getMessage());
        }

        List<StatsTracker.StatsEntry> distances = fetchDistances();
        if (distances != null && !distances.isEmpty()) {
            distances = distances.stream()
                    .sorted((a, b) -> {
                        int cmp = Double.compare(b.stat(), a.stat());
                        return cmp != 0 ? cmp : a.username().compareToIgnoreCase(b.username());
                    })
                    .toList();
        } else {
            source.sendSystemMessage(Component.literal("No distance data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                distances,
                config.getBlacklistedPlayers(),
                config.getUsernameColors()
        );
        formatter.displayLeaderboard(source, ChatFormatting.DARK_AQUA, "Distance: ", ChatFormatting.GOLD, this::formatDistanceStat);
        return 1;
    }

    private List<StatsTracker.StatsEntry> fetchDistances() {
        try {
            PlayerList serverPlayers = StatsCore.getPlayerList();
            for (ServerPlayer player : serverPlayers.getPlayers()) {
                dailyStatsTracker.updatePlayerStat(player);
            }
            List<StatsTracker.StatsEntry> entries = statsTracker.getStats();
            Map<String, Integer> intLimits = config.getIntLimits();

            return entries.stream()
                    .map(e -> {
                        Integer n = intLimits.get(e.username());
                        if (n == null || n <= 0) return e;
                        double extra = (long) n * 2147483647;
                        return new StatsTracker.StatsEntry(e.username(), e.stat() + extra, e.uuid());
                    })
                    .toList();

        } catch (Exception e) {
            sendError("Failed to retrieve distance data: " + e.getMessage());
            LOGGER.error("Failed to retrieve distance data", e);
            return List.of();
        }
    }

    private MutableComponent formatDistanceStat(StatsTracker.StatsEntry entry, int position) {
        String hoverText;

        if (dailyStatsTracker != null) {
            double dailyDistanceCm = dailyStatsTracker.getDailyStat(entry.uuid());
            hoverText = formatDailyDistance(dailyDistanceCm);
        } else {
            hoverText = "N/A";
        }

        double distanceKM = entry.stat() / 100000;

        return Component.literal(formatDistance(distanceKM))
                .withStyle(ChatFormatting.WHITE)
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));
    }

    public static String formatDailyDistance(double distanceCm) {
        double totalMeters = distanceCm / 100.0;
        int km = (int) (totalMeters / 1000);
        int m = (int) (totalMeters % 1000);
        return String.format("%dkm %dm today", km, m);
    }

    public static String formatDistance(double distanceKm) {
        if (distanceKm >= 1000.0) {
            return String.format("%dkm", (int) distanceKm);
        } else if (distanceKm >= 1.0) {
            return String.format("%.2fkm", distanceKm);
        } else {
            double meters = distanceKm * 1000.0;
            return String.format("%dm", (int) meters);
        }
    }

    private void sendError(String message) {
        source.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.RED));
    }
}
