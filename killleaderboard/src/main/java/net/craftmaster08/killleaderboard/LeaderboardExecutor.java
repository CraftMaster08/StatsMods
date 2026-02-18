package net.craftmaster08.killleaderboard;

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
    private final Logger LOGGER;

    LeaderboardExecutor(CommandSourceStack source, Logger LOGGER) {
        this.source = source;
        this.server = KillLeaderboard.getServer();
        this.config = StatsCore.getConfigManager();
        this.LOGGER = LOGGER;

        this.statsTracker = new StatsTracker(server, "minecraft:custom", "player_kills");
        this.dailyStatsTracker = new DailyStatsTracker(Path.of("kills_daily.json"), "daily_kills", statsTracker);

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
            sendError("DailyStatsTracker unavailable; daily kills hover text disabled");
        }

        List<StatsTracker.StatsEntry> kills = fetchKills();
        if (kills != null && !kills.isEmpty()) {
            kills = kills.stream()
                    .sorted((a, b) -> Double.compare(b.stat(), a.stat()))
                    .toList();
        } else {
            source.sendSystemMessage(Component.literal("No kills data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        List<MutableComponent> formattedKills = new ArrayList<>();
        for (StatsTracker.StatsEntry kill : kills) {
            formattedKills.add(formatKillStat(kill));
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                kills,
                config.getBlacklistedPlayers(),
                config.getUsernameColors(),
                formattedKills
        );
        formatter.displayLeaderboard(source, ChatFormatting.DARK_RED, "Kills: ", ChatFormatting.YELLOW);
        return 1;
    }

    private List<StatsTracker.StatsEntry> fetchKills() {
        try {
            PlayerList serverPlayers = StatsCore.getPlayerList();
            for (ServerPlayer player : serverPlayers.getPlayers()) {
                dailyStatsTracker.updatePlayerStat(player);
            }
            return statsTracker.getStats();
        } catch (Exception e) {
            sendError("Failed to retrieve kills data: " + e.getMessage());
            LOGGER.error("Failed to retrieve kills data", e);
            return List.of();
        }
    }

    private MutableComponent formatKillStat(StatsTracker.StatsEntry entry) {
        String singularPlural = entry.stat() == 1 ? "Kill" : "Kills";
        String hoverText;

        if (dailyStatsTracker != null) {
            int dailyKills = (int) dailyStatsTracker.getDailyStat(entry.uuid());
            hoverText = formatDailyKills(dailyKills);
        } else {
            hoverText = "N/A";
        }

        return Component.literal(String.format("%d %s", (int) entry.stat(), singularPlural))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));
    }

    public static String formatDailyKills(int dailyKills) {
        String singularPlural = dailyKills == 1 ? "Kill" : "Kills";
        return String.format("%d %s today", dailyKills, singularPlural);
    }

    private void sendError(String message) {
        source.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.RED));
    }
}
