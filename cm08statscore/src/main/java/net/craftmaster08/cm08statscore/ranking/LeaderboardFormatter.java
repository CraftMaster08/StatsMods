package net.craftmaster08.cm08statscore.ranking;

import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LeaderboardFormatter {
    private static final int BASE_PADDING = 16;
    private static final int BASE_BORDER_LENGTH = 42;

    private final List<StatsTracker.StatsEntry> entries;
    private final Set<String> blacklistedPlayers;
    private final Map<String, ChatFormatting> usernameColors;
    private final List<MutableComponent> formattedStats;

    public LeaderboardFormatter(
            List<StatsTracker.StatsEntry> entries,
            Set<String> blacklistedPlayers,
            Map<String, ChatFormatting> usernameColors,
            List<MutableComponent> formattedStats
    ) {
        this.entries = List.copyOf(entries);
        this.blacklistedPlayers = blacklistedPlayers;
        this.usernameColors = usernameColors;
        this.formattedStats = formattedStats;
    }

    public void displayLeaderboard(CommandSourceStack source, ChatFormatting borderColor, String title, ChatFormatting titleColor) {
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
            formatPlayerEntry(source, filteredStats.get(i), totalPadding, formattedStats.get(i), i + 1);
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

    private void formatPlayerEntry(CommandSourceStack source, StatsTracker.StatsEntry pt, int totalPadding, MutableComponent formattedStat, int position) {
        PodiumRank rank = PodiumRank.fromPosition(position);
        MutableComponent line = rank.formatRank();
        if (rank != PodiumRank.NONE) {
            line = line.append(Component.literal(" "));
        }
        line.append(formatUsername(pt, totalPadding)).append(formattedStat);
        source.sendSystemMessage(line);
    }
}