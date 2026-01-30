package net.craftmaster08.deathleaderboard;

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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        this.server = DeathLeaderboard.getServer();
        this.config = StatsCore.getConfigManager();
        this.LOGGER = LOGGER;

        this.statsTracker = new StatsTracker(server, "minecraft:custom", "deaths");
        this.dailyStatsTracker = new DailyStatsTracker(Path.of("deaths_daily.json"), "daily_deaths", statsTracker);
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
        if (dailyStatsTracker == null) {
            LOGGER.warn("DailyStatsTracker unavailable; daily deaths hover text disabled");
        }

        List<StatsTracker.StatsEntry> deaths = fetchDeaths();
        if (deaths == null) {
            return 0;
        }
        if (deaths.isEmpty()) {
            source.sendSystemMessage(Component.literal("No deaths data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        List<StatsTracker.StatsEntry> playtimes = fetchPlaytimes();
        if (playtimes == null) {
            return 0;
        }
        if (playtimes.isEmpty()) {
            sendError("No playtime data available, PT/Death-Ratio disabled");
        }

        Map<UUID, Double> playtimeMap = playtimes.stream()
                .collect(Collectors.toMap(StatsTracker.StatsEntry::uuid, StatsTracker.StatsEntry::stat));

        List<MutableComponent> formattedDeaths = new ArrayList<>();
        for (int i = 0; i < deaths.size(); i++) {
            formattedDeaths.add(formatDeathStat(deaths.get(i), i + 1, playtimeMap));
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                deaths,
                config.getBlacklistedPlayers(),
                config.getUsernameColors(),
                formattedDeaths
        );
        formatter.displayLeaderboard(source, ChatFormatting.BLACK, "Deaths: ", ChatFormatting.DARK_AQUA, message);
        return 1;
    }

    private List<StatsTracker.StatsEntry> fetchDeaths() {
        try {
            PlayerList serverPlayers = StatsCore.getPlayerList();
            for (ServerPlayer player : serverPlayers.getPlayers()) {
                dailyStatsTracker.updatePlayerStat(player);
            }
            return statsTracker.getStats();
        } catch (Exception e) {
            sendError("Failed to retrieve deaths data: " + e.getMessage());
            LOGGER.error("Failed to retrieve deaths data", e);
            return List.of();
        }
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

    private MutableComponent formatDeathStat(StatsTracker.StatsEntry entry, int position, Map<UUID, Double> playtimeMap) {
        String singularPlural = entry.stat() == 1 ? "Death" : "Deaths";

        double playtime = playtimeMap.getOrDefault(entry.uuid(), 0.0);
        double ratio = (playtime > 0.0 && entry.stat() >= 1) ? playtime / entry.stat() : playtime;

        String hoverText;
        if (dailyStatsTracker != null) {
            int dailyDeaths = (int) dailyStatsTracker.getDailyStat(entry.uuid());
            hoverText = formatDailyDeaths(dailyDeaths);
        } else {
            hoverText = "N/A";
        }

        MutableComponent baseComponent = Component.literal(String.format("%d %s", (int) entry.stat(), singularPlural))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));

        PodiumRank rank = PodiumRank.fromPosition(position);
        message = rank.formatRank();
        if (rank != PodiumRank.NONE) {
            message = message.append(Component.literal(" "));
        }

        String ratioText = String.format("PT/Death-Ratio: %.1f", ratio);
        int ratioIndex = ratioText.indexOf(":") + 2;
        MutableComponent ratioComponent = Component.literal(ratioText.substring(0, ratioIndex))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false));
        ratioComponent.append(Component.literal(ratioText.substring(ratioIndex))
                .withStyle(Style.EMPTY.withColor(rank.getColor()).withBold(false)));

        MutableComponent valueText = Component.literal("");
        valueText = valueText.append(baseComponent).append(Component.literal("    ")).append(ratioComponent);
        return valueText;
    }

    public static String formatDailyDeaths(int dailyDeaths) {
        String singularPlural = dailyDeaths == 1 ? "Death" : "Deaths";

        return String.format("%d %s today", dailyDeaths, singularPlural);
    }

    private void sendError(String message) {
        source.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.RED));
    }
}
