package net.craftmaster08.cm08statscore.ranking;

import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LeaderboardFormatter {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int BASE_PADDING = 16;
    private static final int BASE_BORDER_LENGTH = 42;

    private final List<StatsTracker.StatsEntry> entries;
    private final Set<String> blacklistedPlayers;
    private final Map<String, ChatFormatting> usernameColors;
    private final DailyStatsTracker dailyStatsTracker;
    private final List<MutableComponent> formattedStats;

    public LeaderboardFormatter(
            List<StatsTracker.StatsEntry> entries,
            Set<String> blacklistedPlayers,
            Map<String, ChatFormatting> usernameColors,
            DailyStatsTracker dailyStatsTracker,
            List<MutableComponent> formattedStats
    ) {
        this.entries = List.copyOf(entries);
        this.blacklistedPlayers = blacklistedPlayers;
        this.usernameColors = usernameColors;
        this.dailyStatsTracker = dailyStatsTracker;
        this.formattedStats = formattedStats;
    }

    public void displayLeaderboard(CommandSourceStack source, ChatFormatting borderColor, String title, ChatFormatting titleColor, MutableComponent message) {
        // Filter out blacklisted players
        List<StatsTracker.StatsEntry> filteredStats = entries.stream()
                .filter(pt -> !blacklistedPlayers.contains(pt.username()))
                .toList();

        if (filteredStats.isEmpty()) {
            source.sendSystemMessage(Component.literal("No eligible players to display (all blacklisted or no data)")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        int maxUsernameLength = calculateMaxUsernameLength(filteredStats);
        int totalPadding = Math.max(BASE_PADDING, maxUsernameLength);

        String border = "=".repeat(BASE_BORDER_LENGTH);
        MutableComponent borderComponent = Component.literal(border)
                .withStyle(Style.EMPTY.withColor(borderColor).withBold(true));

        source.sendSystemMessage(borderComponent);
        source.sendSystemMessage(Component.literal(title)
                .withStyle(titleColor));

        for (int i = 0; i < filteredStats.size(); i++) {
            formatPlayerEntry(source, filteredStats.get(i), totalPadding, message, formattedStats.get(i));
            if (i == 2 && filteredStats.size() > 3) {
                source.sendSystemMessage(Component.literal(""));
            }
        }

        source.sendSystemMessage(borderComponent);
    }

    private int calculateMaxUsernameLength(List<StatsTracker.StatsEntry> entries) {
        return entries.stream()
                .map(pt -> pt.username().length())
                .max(Integer::compareTo)
                .orElse(0);
    }

    private MutableComponent formatUsername(StatsTracker.StatsEntry pt, int totalPadding) {
        String username = pt.username();
        String paddedUsername = username + ": " + " ".repeat(Math.max(0, totalPadding - username.length()));
        ChatFormatting usernameColor = usernameColors.getOrDefault(pt.username(), ChatFormatting.WHITE);

        return Component.literal(paddedUsername)
                .withStyle(Style.EMPTY.withColor(usernameColor).withBold(false));
    }


    private MutableComponent formatKillStat(StatsTracker.StatsEntry entry, PodiumRank rank) {
        String singularPlural = entry.stat() == 1 ? "Kill" : "Kills";

        String hoverText;
        if (dailyStatsTracker != null) {
            hoverText = DailyStatsTracker.formatDailyKills(entry.uuid());
        } else {
            hoverText = "N/A";
        }

        MutableComponent baseComponent = Component.literal(String.format("%d %s", (int) entry.stat(), singularPlural))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));

        MutableComponent valueText = Component.literal("");
        valueText = valueText.append(baseComponent);
        return valueText;
    }

    private MutableComponent formatDeathStat(StatsTracker.StatsEntry entry, PodiumRank rank) {
        String singularPlural = entry.stat() == 1 ? "Death" : "Deaths";

        double playtime = playtimeMap.getOrDefault(entry.uuid(), 0.0);
        double ratio = (playtime > 0.0 && entry.stat() >= 1) ? playtime / entry.stat() : playtime;

        String hoverText;
        if (dailyStatsTracker != null) {
            hoverText = DailyStatsTracker.formatDailyDeaths(entry.uuid());
        } else {
            hoverText = "N/A";
        }

        MutableComponent baseComponent = Component.literal(String.format("%d %s", (int) entry.stat(), singularPlural))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));

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

    private MutableComponent formatDistanceStat(StatsTracker.StatsEntry entry) {
        String hoverText;
        if (dailyStatsTracker != null) {
            double dailyDistanceCm = dailyStatsTracker.getDailyDistance(entry.uuid());
            hoverText = DailyStatsTracker.formatDailyDistance(dailyDistanceCm);
        } else {
            hoverText = "N/A";
        }

        MutableComponent base = Component.literal(StatsTracker.formatDistance(entry.stat()));
        return base
                .withStyle(ChatFormatting.WHITE)
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));
    }


    private void formatPlayerEntry(CommandSourceStack source, StatsTracker.StatsEntry pt, int totalPadding, MutableComponent message, MutableComponent formattedStat) {
        message
            .append(formatUsername(pt, totalPadding))
            .append(formattedStat);

        source.sendSystemMessage(message);
    }
}