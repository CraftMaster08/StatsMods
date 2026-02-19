package net.craftmaster08.playtimeleaderboard;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

import java.util.List;

public class LeaderboardExecutor {
    private final CommandSourceStack source;
    private final MinecraftServer server;
    private final ConfigManager config;
    private final DailyStatsTracker dailyStatsTracker;
    private final StatsTracker statsTracker;
    private final Logger LOGGER;

    LeaderboardExecutor(CommandSourceStack source, Logger LOGGER) {
        this.source = source;
        this.server = PlaytimeLeaderboard.getServer();
        this.config = StatsCore.getConfigManager();
        this.LOGGER = LOGGER;
        this.statsTracker = PlaytimeLeaderboard.getStatsTracker();
        this.dailyStatsTracker = PlaytimeLeaderboard.getDailyStatsTracker();

        String resetTime = StatsCore.getConfigManager().dailyResetTime;
        this.dailyStatsTracker.setDailyResetTime(resetTime);
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

        try {
            if (!StatsCore.canUseLeaderboard(source.getPlayerOrException().getUUID())) {
                source.sendSystemMessage(Component.literal("Please wait a few seconds before using this command again.")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            LOGGER.error("No player found {}", e.getMessage());
        }

        List<StatsTracker.StatsEntry> playtimes = fetchPlaytimes();
        if (playtimes != null && !playtimes.isEmpty()) {
            playtimes = playtimes.stream()
                    .sorted((a, b) -> {
                        int cmp = Double.compare(b.stat(), a.stat());
                        return cmp != 0 ? cmp : a.username().compareToIgnoreCase(b.username());
                    })
                    .toList();
        } else {
            source.sendSystemMessage(Component.literal("No playtime data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                playtimes,
                config.getBlacklistedPlayers(),
                config.getUsernameColors()
        );
        formatter.displayLeaderboard(source, ChatFormatting.GOLD, "Playtime: ", ChatFormatting.DARK_GREEN, this::formatPlaytimeStat);
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

        double hours = entry.stat() / 72000.0;

        MutableComponent valueText = HourRange.findRange(hours).formatHours(hours)
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));

        PodiumRank rank = PodiumRank.fromPosition(position);

        if (hours >= 100.0) {
            double days = hours / 24.0;
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