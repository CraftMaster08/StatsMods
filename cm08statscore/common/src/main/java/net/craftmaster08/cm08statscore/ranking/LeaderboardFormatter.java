package net.craftmaster08.cm08statscore.ranking;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class LeaderboardFormatter {
    private static final int PADDING_GAP = 3;
    public static final int PAGE_SIZE = 10;

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
            String commandName,
            int page,
            BiFunction<RankEntry, Integer, MutableComponent> valueFormatter
    ) {
        displayLeaderboard(source, borderColor, title, titleColor, commandName, page, valueFormatter, 0);
    }

    public void displayLeaderboard(
            CommandSourceStack source,
            ChatFormatting borderColor,
            String title,
            ChatFormatting titleColor,
            String commandName,
            int page,
            BiFunction<RankEntry, Integer, MutableComponent> valueFormatter,
            int borderReduction
    ) {
        List<RankEntry> filteredStats = entries.stream()
                .filter(pt -> !blacklistedPlayers.contains(pt.username()))
                .toList();

        if (filteredStats.isEmpty()) {
            source.sendSystemMessage(Component.literal("No eligible players to display (all blacklisted or no data)")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        int totalPages = Math.max(1, (filteredStats.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int clampedPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = (clampedPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredStats.size());
        List<RankEntry> pageEntries = filteredStats.subList(fromIndex, toIndex);

        int maxUsernameLength = calculateMaxUsernameLength(pageEntries);
        int totalPadding = maxUsernameLength + PADDING_GAP;

        List<MutableComponent> lines = new ArrayList<>(pageEntries.size());
        for (int i = 0; i < pageEntries.size(); i++) {
            RankEntry entry = pageEntries.get(i);
            int rank = fromIndex + i + 1;
            MutableComponent valueText = valueFormatter.apply(entry, rank);
            lines.add(buildPlayerLine(entry, totalPadding, valueText, rank));
        }

        int borderLength = Math.max(title.length(), maxLineLength(lines) - borderReduction);
        MutableComponent borderComponent = Component.literal("=".repeat(borderLength))
                .withStyle(Style.EMPTY.withColor(borderColor).withBold(true));

        source.sendSystemMessage(borderComponent);
        source.sendSystemMessage(Component.literal(title)
                .withStyle(Style.EMPTY.withColor(titleColor).withBold(false)));

        for (int i = 0; i < lines.size(); i++) {
            source.sendSystemMessage(lines.get(i));

            if (clampedPage == 1 && i == 2 && lines.size() > 3) {
                source.sendSystemMessage(Component.literal(""));
            }
        }

        if (totalPages > 1) {
            source.sendSystemMessage(buildPageControls(commandName, clampedPage, totalPages));
        }

        source.sendSystemMessage(borderComponent);
    }

    private MutableComponent buildPageControls(String commandName, int page, int totalPages) {
        MutableComponent line = Component.literal("");
        line.append(pageArrow("<-- ", commandName, page - 1, page > 1));
        line.append(Component.literal("Page " + page + " of " + totalPages)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false)));
        line.append(pageArrow(" -->", commandName, page + 1, page < totalPages));
        return line;
    }

    private MutableComponent pageArrow(String symbol, String commandName, int targetPage, boolean enabled) {
        if (!enabled) {
            return Component.literal(symbol).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withBold(false));
        }
        String runCommand = commandName + " " + targetPage;
        return Component.literal(symbol).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, runCommand))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to go to page " + targetPage))));
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
