package net.craftmaster08.playtimeleaderboard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.ranking.LeaderboardFormatter;
import net.craftmaster08.cm08statscore.ranking.PodiumRank;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PlaytimeRunCommand {
    private static final Logger LOGGER = LogManager.getLogger(PlaytimeRunCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("playtime")
                .requires(source -> source.hasPermission(0))
                .executes(context -> new LeaderboardExecutor(context.getSource()).execute());

        try {
            dispatcher.register(command);
            LOGGER.info("Successfully registered /playtime command");
        } catch (Exception e) {
            LOGGER.error("Failed to register /playtime command", e);
        }
    }

    private static class LeaderboardExecutor {
        private final CommandSourceStack source;
        private final MinecraftServer server;
        private final ConfigManager config;
        private final DailyStatsTracker dailyStatsTracker;
        private MutableComponent message = null;

        LeaderboardExecutor(CommandSourceStack source) {
            this.source = source;
            this.server = PlaytimeLeaderboard.getServer();
            this.config = StatsCore.getConfigManager();
            this.dailyStatsTracker = new DailyStatsTracker(server, Path.of("playtime_daily.json"), Stats.PLAY_TIME, "play_time", "minecraft:custom", "daily_playtimes");
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
                LOGGER.warn("DailyPlaytimeTracker unavailable; daily playtime hover text disabled");
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
                    dailyStatsTracker,
                    formattedPlaytimes
            );
            formatter.displayLeaderboard(source, ChatFormatting.GOLD, "Playtime: ", ChatFormatting.DARK_GREEN, message);
            return 1;
        }

        private List<StatsTracker.StatsEntry> fetchPlaytimes() {
            try {
                return StatsTracker.getStats(server, "minecraft:custom", "play_time");
            } catch (Exception e) {
                sendError("Failed to retrieve playtime data: " + e.getMessage());
                LOGGER.error("Failed to retrieve playtime data", e);
                return null;
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
}

    enum HourRange {
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