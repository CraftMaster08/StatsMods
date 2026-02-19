package net.craftmaster08.deathleaderboard;

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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LeaderboardExecutor {
    private final CommandSourceStack source;
    private final MinecraftServer server;
    private final ConfigManager config;
    private final StatsTracker statsTracker;
    private final StatsTracker playtimeTracker;
    private final DailyStatsTracker dailyStatsTracker;
    private final Logger LOGGER;

    LeaderboardExecutor(CommandSourceStack source, Logger LOGGER) {
        this.source = source;
        this.server = DeathLeaderboard.getServer();
        this.config = StatsCore.getConfigManager();
        this.LOGGER = LOGGER;
        this.statsTracker = DeathLeaderboard.getStatsTracker();
        this.playtimeTracker = DeathLeaderboard.getPlaytimeTracker();
        this.dailyStatsTracker = DeathLeaderboard.getDailyStatsTracker();

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
        if (statsTracker == null || playtimeTracker == null) {
            sendError("StatsTracker(s) not initialized");
            return 0;
        }
        if (dailyStatsTracker == null) {
            LOGGER.warn("DailyStatsTracker unavailable; daily deaths hover text disabled");
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

        List<StatsTracker.StatsEntry> deaths = fetchDeaths();
        if (deaths != null && !deaths.isEmpty()) {
            deaths = deaths.stream()
                    .sorted((a, b) -> {
                        int cmp = Double.compare(b.stat(), a.stat());
                        return cmp != 0 ? cmp : a.username().compareToIgnoreCase(b.username());
                    })
                    .toList();
        } else {
            source.sendSystemMessage(Component.literal("No deaths data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
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
            source.sendSystemMessage(Component.literal("No playtime data available. PT/Death-Ratio disabled")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        Map<UUID, Double> playtimeMap = playtimes.stream()
                .collect(Collectors.toMap(StatsTracker.StatsEntry::uuid, StatsTracker.StatsEntry::stat));

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                deaths,
                config.getBlacklistedPlayers(),
                config.getUsernameColors()
        );
        formatter.displayLeaderboard(source, ChatFormatting.BLACK, "Deaths: ", ChatFormatting.DARK_AQUA, (entry, position) -> formatDeathStat(entry, position, playtimeMap));
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
            return playtimeTracker.getStats();
        } catch (Exception e) {
            sendError("Failed to retrieve playtime data: " + e.getMessage());
            LOGGER.error("Failed to retrieve playtime data", e);
            return List.of();
        }
    }

    private MutableComponent formatDeathStat(StatsTracker.StatsEntry entry, int position, Map<UUID, Double> playtimeMap) {
        String singularPlural = entry.stat() == 1 ? "Death" : "Deaths";

        double playtime = playtimeMap.getOrDefault(entry.uuid(), 0.0);
        playtime /= 72000.0;
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
