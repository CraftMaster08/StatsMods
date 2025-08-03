package net.craftmaster08.distanceleaderboard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.linux.Stat;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DistanceRunCommand {
    private static final Logger LOGGER = LogManager.getLogger(DistanceRunCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("distance")
                .requires(source -> source.hasPermission(0))
                .executes(context -> new LeaderboardExecutor(context.getSource()).execute());

        try {
            dispatcher.register(command);
            LOGGER.info("Successfully registered /distance command");
        } catch (Exception e) {
            LOGGER.error("Failed to register /distance command", e);
        }
    }

    private static class LeaderboardExecutor {
        private final CommandSourceStack source;
        private final MinecraftServer server;
        private final ConfigManager config;
        private final DailyStatsTracker dailyStatsTracker;

        LeaderboardExecutor(CommandSourceStack source) {
            this.source = source;
            this.server = DistanceLeaderboard.getServer();
            this.config = StatsCore.getConfigManager();
            this.dailyStatsTracker = StatsCore.getDailyStatsTracker();
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
                sendError("DailyDistanceTracker unavailable; daily distance hover text disabled");
            }

            List<StatsTracker.PlayerDistance> distances = fetchDistances();
            if (distances == null) {
                return 0;
            }
            if (distances.isEmpty()) {
                source.sendSystemMessage(Component.literal("No distance data available")
                        .withStyle(ChatFormatting.YELLOW));
                return 1;
            }

            LeaderboardFormatter formatter = new LeaderboardFormatter(
                    distances,
                    config.getBlacklistedPlayers(),
                    config.getUsernameColors(),
                    dailyStatsTracker
            );
            formatter.displayLeaderboard(source);
            return 1;
        }

        private List<StatsTracker.PlayerDistance> fetchDistances() {
            try {
                return StatsTracker.getOverallDistance(server);
            } catch (Exception e) {
                sendError("Failed to retrieve distance data: " + e.getMessage());
                LOGGER.error("Failed to retrieve distance data", e);
                return null;
            }
        }

        private void sendError(String message) {
            source.sendSystemMessage(Component.literal(message)
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static class LeaderboardFormatter {
        private static final int BASE_PADDING = 16;
        private static final int RANK_LENGTH = 3;

        private final List<StatsTracker.PlayerDistance> distances;
        private final Set<String> blacklistedPlayers;
        private final Map<String, ChatFormatting> usernameColors;
        private final DailyStatsTracker dailyStatsTracker;

        LeaderboardFormatter(
                List<StatsTracker.PlayerDistance> distances,
                Set<String> blacklistedPlayers,
                Map<String, ChatFormatting> usernameColors,
                DailyStatsTracker dailyStatsTracker
        ) {
            this.distances = distances;
            this.blacklistedPlayers = blacklistedPlayers;
            this.usernameColors = usernameColors;
            this.dailyStatsTracker = dailyStatsTracker;
        }

        void displayLeaderboard(CommandSourceStack source) {
            // Filter out blacklisted players
            List<StatsTracker.PlayerDistance> filteredDistances = distances.stream()
                    .filter(pt -> !blacklistedPlayers.contains(pt.username()))
                    .toList();

            if (filteredDistances.isEmpty()) {
                source.sendSystemMessage(Component.literal("No eligible players to display (all blacklisted or no data)")
                        .withStyle(ChatFormatting.YELLOW));
                return;
            }

            int maxUsernameLength = calculateMaxUsernameLength(filteredDistances);
            int totalPadding = Math.max(BASE_PADDING, maxUsernameLength + RANK_LENGTH);
            int maxLineLength = calculateMaxLineLength(totalPadding, filteredDistances);

            String border = "=".repeat(maxLineLength + 3);
            MutableComponent borderComponent = Component.literal(border)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withBold(true));

            source.sendSystemMessage(borderComponent);
            source.sendSystemMessage(Component.literal("Distance traveled:")
                    .withStyle(ChatFormatting.GOLD));

            for (int i = 0; i < filteredDistances.size(); i++) {
                formatPlayerEntry(source, filteredDistances.get(i), i + 1, totalPadding);
                if (i == 2 && filteredDistances.size() > 3) {
                    source.sendSystemMessage(Component.literal(""));
                }
            }

            source.sendSystemMessage(borderComponent);
        }

        private int calculateMaxUsernameLength(List<StatsTracker.PlayerDistance> distances) {
            return distances.stream()
                    .map(pt -> (pt.username() + ":").length())
                    .max(Integer::compareTo)
                    .orElse(0);
        }

        private int calculateMaxLineLength(int totalPadding, List<StatsTracker.PlayerDistance> distances) {
            int maxLineLength = 0;
            for (int i = 0; i < distances.size(); i++) {
                int lineLength = 0;
                PodiumRank rank = PodiumRank.fromPosition(i + 1);
                if (rank != PodiumRank.NONE) {
                    lineLength += RANK_LENGTH;
                }
                lineLength += totalPadding;

                double distanceKm = distances.get(i).distanceKm();
                String distanceText = formatDistance(distanceKm);
                lineLength += distanceText.length();
                maxLineLength = Math.max(maxLineLength, lineLength);
            }
            return Math.max(maxLineLength, 9);
        }

        private void formatPlayerEntry(CommandSourceStack source, StatsTracker.PlayerDistance pt, int position, int totalPadding) {
            PodiumRank rank = PodiumRank.fromPosition(position);
            MutableComponent message = rank.formatRank();
            if (rank != PodiumRank.NONE) {
                message = message.append(Component.literal(" "));
            }

            String username = pt.username() + ":";
            int usernamePadding = (rank != PodiumRank.NONE) ? totalPadding - RANK_LENGTH : totalPadding;
            String paddedUsername = username + " ".repeat(Math.max(0, usernamePadding - username.length()));
            ChatFormatting usernameColor = usernameColors.getOrDefault(pt.username(), ChatFormatting.WHITE);
            MutableComponent usernameComponent = Component.literal(paddedUsername)
                    .withStyle(Style.EMPTY.withColor(usernameColor).withBold(false));

            String distanceText = formatDistance(pt.distanceKm());
            MutableComponent distanceComponent = Component.literal("    " + distanceText) // Add 4 spaces before distance
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false));

            if (dailyStatsTracker != null) {
                double dailyDistanceCm = dailyStatsTracker.getDailyDistance(pt.uuid());
                String dailyText = DailyStatsTracker.formatDailyDistance(dailyDistanceCm);
                distanceComponent = distanceComponent.withStyle(distanceComponent.getStyle().withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(dailyText))
                ));
            } else {
                distanceComponent = distanceComponent.withStyle(distanceComponent.getStyle().withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Daily distance unavailable"))
                ));
            }

            message = message.append(usernameComponent).append(distanceComponent);

            source.sendSystemMessage(message);
        }

        private String formatDistance(double distanceKm) {
            if (distanceKm >= 1000.0) {
                return String.format("%dkm", (int) distanceKm);
            } else if (distanceKm >= 1.0) {
                return String.format("%.2fkm", distanceKm);
            } else {
                double meters = distanceKm * 1000.0;
                return String.format("%dm", (int) meters);
            }
        }
    }

    private enum PodiumRank {
        FIRST(1, ChatFormatting.GOLD, true),
        SECOND(2, ChatFormatting.WHITE, true),
        THIRD(3, ChatFormatting.DARK_PURPLE, true),
        NONE(0, ChatFormatting.WHITE, false);

        private final int rank;
        private final ChatFormatting color;
        private final boolean isBold;

        PodiumRank(int rank, ChatFormatting color, boolean isBold) {
            this.rank = rank;
            this.color = color;
            this.isBold = isBold;
        }

        public static PodiumRank fromPosition(int position) {
            return switch (position) {
                case 1 -> FIRST;
                case 2 -> SECOND;
                case 3 -> THIRD;
                default -> NONE;
            };
        }

        public MutableComponent formatRank() {
            if (this == NONE) {
                return Component.literal("");
            }
            return Component.literal(rank + ".")
                    .withStyle(Style.EMPTY.withColor(color).withBold(isBold));
        }
    }
}