package net.craftmaster08.playtimeleaderboard;

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
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardExecutor {
    private final CommandSourceStack source;
    private final MinecraftServer server;
    private final ConfigManager config;
    private final DailyStatsTracker dailyStatsTracker;
    private final StatsTracker statsTracker;
    private MutableComponent message = null;
    private final Logger LOGGER;

    LeaderboardExecutor(CommandSourceStack source, Logger LOGGER) {
        this.source = source;
        this.server = PlaytimeLeaderboard.getServer();
        this.config = StatsCore.getConfigManager();
        this.LOGGER = LOGGER;

        this.statsTracker = new StatsTracker(server, "minecraft:custom", "play_time");
        this.dailyStatsTracker = new DailyStatsTracker(Path.of("playtime_daily.json"), "daily_playtimes", statsTracker);
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
            LOGGER.warn("DailyStatsTracker unavailable; daily playtime hover text disabled");
        }

        List<StatsTracker.StatsEntry> playtimes = fetchPlaytimes();
        if (playtimes == null) {
            return 0;
        }
        if (playtimes.isEmpty()) {
            source.sendSystemMessage(Component.literal("No playtime data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        List<MutableComponent> formattedPlaytimes = new ArrayList<>();
        for (int i = 0; i < playtimes.size(); i++) {
            formattedPlaytimes.add(formatPlaytimeStat(playtimes.get(i), i + 1));
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                playtimes,
                config.getBlacklistedPlayers(),
                config.getUsernameColors(),
                formattedPlaytimes
        );
        formatter.displayLeaderboard(source, ChatFormatting.GOLD, "Playtime: ", ChatFormatting.DARK_GREEN, message);
        return 1;
    }

    private List<StatsTracker.StatsEntry> fetchPlaytimes() {
        try {
            PlayerList serverPlayers = StatsCore.getPlayerList();
            for (ServerPlayer player : serverPlayers.getPlayers()) {
                dailyStatsTracker.updatePlayerStat(player);
            }
            return statsTracker.getStats();
        } catch (Exception e) {
            sendError("Failed to retrieve playtime data: " + e.getMessage());
            LOGGER.error("Failed to retrieve playtime data", e);
            return List.of();
        }
    }

    private MutableComponent formatPlaytimeStat(StatsTracker.StatsEntry entry, int position) {
        String hoverText;

        if (dailyStatsTracker != null) {
            double dailyTicks = dailyStatsTracker.getDailyStat(entry.uuid());
            hoverText = formatDailyPlaytime(dailyTicks);
        } else {
            hoverText = "N/A";
        }

        MutableComponent valueText = HourRange.findRange(entry.stat()).formatHours(entry.stat())
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));

        PodiumRank rank = PodiumRank.fromPosition(position);
        message = rank.formatRank();
        if (rank != PodiumRank.NONE) {
            message = message.append(Component.literal(" "));
        }

        if (entry.stat() >= 100.0) {
            double days = entry.stat() / 24.0;
            valueText = valueText.append(Component.literal(String.format("    (%.2fd)", days))
                    .withStyle(Style.EMPTY.withColor(rank.getColor()).withBold(true)));
        }
        return valueText;
    }

    public static String formatDailyPlaytime(double ticks) {
        double hours = ticks / 72000;

        double totalSecondsDouble = hours * 3600.0;
        int h = (int) (totalSecondsDouble / 3600);
        double remainingSeconds = totalSecondsDouble % 3600;
        int m = (int) (remainingSeconds / 60);
        int s = (int) (remainingSeconds % 60);
        return String.format("%dh %dmin %dsec today", h, m, s);
    }

    private void sendError(String message) {
        source.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.RED));
    }
}