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

    private final List<StatsEntry> entries;
    private final Set<String> blacklistedPlayers;
    private final Map<String, ChatFormatting> usernameColors;
    private final DailyStatsTracker dailyStatsTracker;
    private final Map<UUID, Double> playtimeMap;
    private final StatsType type;

    public enum StatsType {DEATHS, DISTANCE, PLAYTIME}


    public LeaderboardFormatter(
            List<StatsEntry> entries,
            StatsType type,
            Set<String> blacklistedPlayers,
            Map<String, ChatFormatting> usernameColors,
            DailyStatsTracker dailyStatsTracker,
            Map<UUID, Double> playtimeMap
    ) {
        this.entries = List.copyOf(entries);
        this.type = type;
        this.blacklistedPlayers = blacklistedPlayers;
        this.usernameColors = usernameColors;
        this.dailyStatsTracker = dailyStatsTracker;
        this.playtimeMap = playtimeMap;
    }

    String getTitleFromType(StatsType type) {
        return switch (type) {
            case DEATHS -> "Deaths:";
            case DISTANCE -> "Distance traveled: ";
            case PLAYTIME -> "Playtime: ";
        };
    }

    public void displayLeaderboard(CommandSourceStack source, ChatFormatting borderColor, ChatFormatting titleColor, StatsType type) {
        // Filter out blacklisted players
        List<StatsEntry> filteredStats = entries.stream()
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
        source.sendSystemMessage(Component.literal(getTitleFromType(type))
                .withStyle(titleColor));

        for (int i = 0; i < filteredStats.size(); i++) {
            formatPlayerEntry(source, filteredStats.get(i), i + 1, totalPadding);
            if (i == 2 && filteredStats.size() > 3) {
                source.sendSystemMessage(Component.literal(""));
            }
        }

        source.sendSystemMessage(borderComponent);
    }

    private int calculateMaxUsernameLength(List<StatsEntry> entries) {
        return entries.stream()
                .map(pt -> pt.username().length())
                .max(Integer::compareTo)
                .orElse(0);
    }

    private MutableComponent formatUsername(StatsEntry pt, int totalPadding) {
        String username = pt.username();
        String paddedUsername = username + ": " + " ".repeat(Math.max(0, totalPadding - username.length()));
        ChatFormatting usernameColor = usernameColors.getOrDefault(pt.username(), ChatFormatting.WHITE);

        return Component.literal(paddedUsername)
                .withStyle(Style.EMPTY.withColor(usernameColor).withBold(false));
    }

    private MutableComponent formatDeathStat(StatsEntry entry, PodiumRank rank) {
        var d = (StatsTracker.PlayerDeaths) entry;

        String singularPlural = d.deaths() == 1 ? "Death" : "Deaths";

        double playtime = playtimeMap.getOrDefault(entry.uuid(), 0.0);
        double ratio = (playtime > 0.0 && d.deaths() >= 1) ? playtime / d.deaths() : 0.0;

        String hoverText;
        if (dailyStatsTracker != null) {
            hoverText = DailyStatsTracker.formatDailyDeaths(entry.uuid());
        } else {
            hoverText = "N/A";
        }

        MutableComponent baseComponent = Component.literal(String.format("%d %s", d.deaths(), singularPlural))
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

    private MutableComponent formatDistanceStat(StatsEntry entry) {
        var d = (StatsTracker.PlayerDistance) entry;

        String hoverText;
        if (dailyStatsTracker != null) {
            double dailyDistanceCm = dailyStatsTracker.getDailyDistance(entry.uuid());
            hoverText = DailyStatsTracker.formatDailyDistance(dailyDistanceCm);
        } else {
            hoverText = "N/A";
        }

        MutableComponent base = Component.literal(StatsTracker.formatDistance(d.distanceKm()));
        return base
                .withStyle(ChatFormatting.WHITE)
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));
    }

    private MutableComponent formatPlaytimeStat(StatsEntry entry, PodiumRank rank) {
        LOGGER.info("entered playtime if");
        var p = (StatsTracker.PlayerPlaytime) entry;

        String hoverText;
        if (dailyStatsTracker != null) {
            double dailyHours = dailyStatsTracker.getDailyPlaytime(entry.uuid());
            hoverText = DailyStatsTracker.formatDailyPlaytime(dailyHours);
        } else {
            hoverText = "N/A";
        }

        MutableComponent valueText = HourRange.findRange(p.playtime()).formatHours(p.playtime())
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));

        LOGGER.info("valueText: {}", valueText);


        if (p.playtime() >= 100.0) {
            double days = p.playtime() / 24.0;
            valueText = valueText.append(Component.literal(String.format("    (%.2fd)", days))
                    .withStyle(Style.EMPTY.withColor(rank.getColor()).withBold(true)));
        }
        return valueText;
    }

    private void formatPlayerEntry(CommandSourceStack source, StatsEntry pt, int position, int totalPadding) {
        PodiumRank rank = PodiumRank.fromPosition(position);
        MutableComponent message = rank.formatRank();
        if (rank != PodiumRank.NONE) {
            message = message.append(Component.literal(" "));
        }

        switch (type) {
            case DEATHS -> message
                    .append(formatUsername(pt, totalPadding))
                    .append(formatDeathStat(pt, rank));
            case DISTANCE -> message
                    .append(formatUsername(pt, totalPadding)
                    .append(formatDistanceStat(pt)));
            case PLAYTIME -> message
                    .append(formatUsername(pt, totalPadding)
                    .append(formatPlaytimeStat(pt, rank)));
            default -> {
                message = Component.literal("N/A");
                LOGGER.error("Invalid StatsType");
            }
        }
        source.sendSystemMessage(message);
    }

    private enum HourRange {
        UNDER_100(0, 100, new ChatFormatting[]{ChatFormatting.GRAY}, ChatFormatting.GRAY, null, null),
        H_100_199(100, 200, new ChatFormatting[]{ChatFormatting.WHITE}, ChatFormatting.WHITE, null, null),
        H_200_299(200, 300, new ChatFormatting[]{ChatFormatting.GOLD}, ChatFormatting.GOLD, null, null),
        H_300_399(300, 400, new ChatFormatting[]{ChatFormatting.AQUA}, ChatFormatting.AQUA, null, null),
        H_400_499(400, 500, new ChatFormatting[]{ChatFormatting.DARK_GREEN}, ChatFormatting.DARK_GREEN, null, null),
        H_500_599(500, 600, new ChatFormatting[]{ChatFormatting.DARK_AQUA}, ChatFormatting.DARK_AQUA, null, null),
        H_600_699(600, 700, new ChatFormatting[]{ChatFormatting.DARK_RED}, ChatFormatting.DARK_RED, null, null),
        H_700_799(700, 800, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, ChatFormatting.LIGHT_PURPLE, null, null),
        H_800_899(800, 900, new ChatFormatting[]{ChatFormatting.BLUE}, ChatFormatting.BLUE, null, null),
        H_900_999(900, 1000, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, ChatFormatting.DARK_PURPLE, null, null),
        H_1000_1099(1000, 1100, new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.AQUA}, ChatFormatting.LIGHT_PURPLE, "✫", ChatFormatting.RED),
        H_1100_1199(1100, 1200, new ChatFormatting[]{ChatFormatting.WHITE}, ChatFormatting.GRAY, "✪", ChatFormatting.GRAY),
        H_1200_1299(1200, 1300, new ChatFormatting[]{ChatFormatting.YELLOW}, ChatFormatting.GRAY, "✪", ChatFormatting.GOLD),
        H_1300_1399(1300, 1400, new ChatFormatting[]{ChatFormatting.AQUA}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_AQUA),
        H_1400_1499(1400, 1500, new ChatFormatting[]{ChatFormatting.GREEN}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_GREEN),
        H_1500_1599(1500, 1600, new ChatFormatting[]{ChatFormatting.DARK_AQUA}, ChatFormatting.GRAY, "✪", ChatFormatting.BLUE),
        H_1600_1699(1600, 1700, new ChatFormatting[]{ChatFormatting.RED}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_RED),
        H_1700_1799(1700, 1800, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_PURPLE),
        H_1800_1899(1800, 1900, new ChatFormatting[]{ChatFormatting.BLUE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_BLUE),
        H_1900_1999(1900, 2000, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_GRAY),
        H_2000_2099(2000, 2100, new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.GRAY}, ChatFormatting.DARK_GRAY, "✪", ChatFormatting.GRAY);

        private final double minHours;
        private final double maxHours;
        private final ChatFormatting[] hoursColors;
        private final ChatFormatting hColor;
        private final String starSymbol;
        private final ChatFormatting starColor;

        HourRange(double minHours, double maxHours, ChatFormatting[] hoursColors, ChatFormatting hColor, String starSymbol, ChatFormatting starColor) {
            this.minHours = minHours;
            this.maxHours = maxHours;
            this.hoursColors = hoursColors;
            this.hColor = hColor;
            this.starSymbol = starSymbol;
            this.starColor = starColor;
        }

        public static HourRange findRange(double playtime) {
            for (HourRange range : values()) {
                if (playtime >= range.minHours && playtime < range.maxHours) {
                    return range;
                }
            }
            return H_2000_2099; //change to unter 100
        }

        public MutableComponent formatHours(double playtime) {
            String hoursText = playtime >= 1000.0 ? String.format("%d", (int) playtime) : String.format("%.2f", playtime);
            MutableComponent component = Component.literal("");

            if (starSymbol != null) {
                component.append(Component.literal(starSymbol + " ")
                        .withStyle(Style.EMPTY.withColor(starColor).withBold(false)));
            }

            if (hoursColors.length > 1) {
                String[] chars = hoursText.split("");
                for (int i = 0; i < chars.length; i++) {
                    ChatFormatting color = i < hoursColors.length ? hoursColors[i] : ChatFormatting.WHITE;
                    component.append(Component.literal(chars[i])
                            .withStyle(Style.EMPTY.withColor(color).withBold(false)));
                }
            } else {
                component.append(Component.literal(hoursText)
                        .withStyle(Style.EMPTY.withColor(hoursColors[0]).withBold(false)));
            }

            component.append(Component.literal("h")
                    .withStyle(Style.EMPTY.withColor(hColor).withBold(false)));

            return component;
        }
    }
}