package net.craftmaster08.distanceleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.ranking.LeaderboardFormatter;
import net.craftmaster08.cm08statscore.ranking.PodiumRank;
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

import java.nio.file.Path;
import java.util.*;

public class LeaderboardExecutor {
    private final CommandSourceStack source;
    private final MinecraftServer server;
    private final ConfigManager config;
    private final DailyStatsTracker dailyStatsTracker;
    private final StatsTracker statsTracker;
    private MutableComponent message = null;
    private final Logger LOGGER;

    final String[] STRING_DISTANCE_STATS = {
            "walk_one_cm", "sprint_one_cm", "crouch_one_cm", "swim_one_cm",
            "fall_one_cm", "climb_one_cm", "fly_one_cm", "walk_on_water_one_cm",
            "walk_under_water_one_cm", "minecart_one_cm", "boat_one_cm", "pig_one_cm",
            "horse_one_cm", "aviate_one_cm", "strider_one_cm"
    };

    public LeaderboardExecutor(CommandSourceStack source, Logger LOGGER) {
        this.source = source;
        this.server = DistanceLeaderboard.getServer();
        this.config = StatsCore.getConfigManager();
        this.LOGGER = LOGGER;

        this.statsTracker = new StatsTracker(server, "minecraft:custom", STRING_DISTANCE_STATS);
        this.dailyStatsTracker = new DailyStatsTracker(Path.of("distance_daily.json"), "daily_distances", statsTracker);
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

        List<StatsTracker.StatsEntry> distances = fetchDistances();
        if (distances == null) {
            return 0;
        }
        if (distances.isEmpty()) {
            source.sendSystemMessage(Component.literal("No distance data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        List<MutableComponent> formattedDistances = new ArrayList<>();
        for (int i = 0; i < distances.size(); i++) {
            formattedDistances.add(formatDistanceStat(distances.get(i), i + 1));
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                distances,
                config.getBlacklistedPlayers(),
                config.getUsernameColors(),
                formattedDistances
        );
        formatter.displayLeaderboard(source, ChatFormatting.DARK_AQUA, "Distance: ", ChatFormatting.GOLD, message);
        return 1;
    }

    private List<StatsTracker.StatsEntry> fetchDistances() {
        try {
            PlayerList serverPlayers = StatsCore.getPlayerList();
            for (ServerPlayer player : serverPlayers.getPlayers()) {
                dailyStatsTracker.updatePlayerStat(player);
            }

            return statsTracker.getStats();
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

        MutableComponent valueText = Component.literal(formatDistance(entry.stat()))
                .withStyle(ChatFormatting.WHITE)
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));

        PodiumRank rank = PodiumRank.fromPosition(position);
        message = rank.formatRank();
        if (rank != PodiumRank.NONE) {
            message = message.append(Component.literal(" "));
        }

        return valueText;
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
