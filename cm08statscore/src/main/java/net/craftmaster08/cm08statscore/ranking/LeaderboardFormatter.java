package net.craftmaster08.cm08statscore.ranking;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class LeaderboardFormatter {
    private static final int PADDING_GAP = 3;

    private final List<RankEntry> entries;
    private final Set<String> blacklistedPlayers;
    private final Map<String, ChatFormatting> usernameColors;

    public LeaderboardFormatter(
            List<RankEntry> entries,
            Set<String> blacklistedPlayers,
            Map<String, ChatFormatting> usernameColors
    ) {
        this.entries = List.copyOf(entries);
        this.blacklistedPlayers = blacklistedPlayers;
        this.usernameColors = usernameColors;
    }

    public void displayLeaderboard(
            CommandSourceStack source,
            ChatFormatting borderColor,
            String title,
            ChatFormatting titleColor,
            BiFunction<RankEntry, Integer, MutableComponent> valueFormatter
    ) {
        List<RankEntry> filteredStats = entries.stream()
                .filter(pt -> !blacklistedPlayers.contains(pt.username()))
                .toList();

        if (filteredStats.isEmpty()) {
            source.sendSystemMessage(Component.literal("No eligible players to display (all blacklisted or no data)")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        int maxUsernameLength = calculateMaxUsernameLength(filteredStats);
        int totalPadding = maxUsernameLength + PADDING_GAP;

        List<MutableComponent> lines = new ArrayList<>(filteredStats.size());
        for (int i = 0; i < filteredStats.size(); i++) {
            RankEntry entry = filteredStats.get(i);
            MutableComponent valueText = valueFormatter.apply(entry, i + 1);
            lines.add(buildPlayerLine(entry, totalPadding, valueText, i + 1));
        }

        int borderLength = Math.max(title.length(), maxLineLength(lines));
        MutableComponent borderComponent = Component.literal("=".repeat(borderLength))
                .withStyle(Style.EMPTY.withColor(borderColor).withBold(true));

        source.sendSystemMessage(borderComponent);
        source.sendSystemMessage(Component.literal(title)
                .withStyle(Style.EMPTY.withColor(titleColor).withBold(false)));

        for (int i = 0; i < lines.size(); i++) {
            source.sendSystemMessage(lines.get(i));

            if (i == 2 && lines.size() > 3) {
                source.sendSystemMessage(Component.literal(""));
            }
        }

        source.sendSystemMessage(borderComponent);
    }

    private int maxLineLength(List<MutableComponent> lines) {
        int max = 0;
        for (MutableComponent line : lines) {
            max = Math.max(max, line.getString().length());
        }
        return max;
    }

    private int calculateMaxUsernameLength(List<RankEntry> entries) {
        return entries.stream()
                .mapToInt(pt -> pt.username().length())
                .max()
                .orElse(0);
    }

    private MutableComponent formatUsername(RankEntry pt, int totalPadding) {
        String username = pt.username();
        String padded = username + ": " + " ".repeat(Math.max(0, totalPadding - username.length()));
        ChatFormatting color = usernameColors.getOrDefault(username, ChatFormatting.WHITE);

        return Component.literal(padded)
                .withStyle(Style.EMPTY.withColor(color).withBold(false));
    }

    private MutableComponent buildPlayerLine(RankEntry pt, int totalPadding, MutableComponent formattedStat, int position) {
        PodiumRank rank = PodiumRank.fromPosition(position);
        MutableComponent line = rank.formatRank();
        if (rank != PodiumRank.NONE) {
            line = line.append(Component.literal(" "));
        }
        return line.append(formatUsername(pt, totalPadding)).append(formattedStat);
    }
}
