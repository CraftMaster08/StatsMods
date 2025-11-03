package net.craftmaster08.deathleaderboard;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeathRunCommand {
    private static final Logger LOGGER = LogManager.getLogger(DeathRunCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("deaths")
                .requires(source -> source.hasPermission(0))
                .executes(context -> new LeaderboardExecutor(context.getSource()).execute());

        try {
            dispatcher.register(command);
            LOGGER.info("Successfully registered /deaths command");
        } catch (Exception e) {
            LOGGER.error("Failed to register /deaths command", e);
        }
    }

    private static class LeaderboardExecutor {
        private final CommandSourceStack source;
        private final MinecraftServer server;
        private final ConfigManager config;
        private final DailyStatsTracker dailyStatsTracker;

        LeaderboardExecutor(CommandSourceStack source) {
            this.source = source;
            this.server = DeathLeaderboard.getServer();
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
                sendError("DailyStatsTracker unavailable; daily deaths hover text disabled");
            }

            List<StatsTracker.PlayerDeaths> deaths = fetchDeaths();
            if (deaths == null) {
                return 0;
            }
            if (deaths.isEmpty()) {
                source.sendSystemMessage(Component.literal("No deaths data available")
                        .withStyle(ChatFormatting.YELLOW));
                return 1;
            }

            List<StatsTracker.PlayerPlaytime> playtimes = fetchPlaytimes();
            if (playtimes == null) {
                return 0;
            }

            Map<UUID, Double> playtimeMap = playtimes.stream()
                    .collect(Collectors.toMap(StatsTracker.PlayerPlaytime::uuid, StatsTracker.PlayerPlaytime::playtime));

            LeaderboardFormatter formatter = new LeaderboardFormatter(
                    deaths,
                    config.getBlacklistedPlayers(),
                    config.getUsernameColors(),
                    dailyStatsTracker,
                    playtimeMap
            );
            formatter.displayLeaderboard(source);
            return 1;
        }

        private List<StatsTracker.PlayerDeaths> fetchDeaths() {
            try {
                // refresh daily deaths
                PlayerList serverPlayers = StatsCore.getPlayerList();
                for (ServerPlayer player : serverPlayers.getPlayers()) {
                    dailyStatsTracker.updatePlayerDeaths(player);
                }

                return StatsTracker.getOverallDeaths(server);
            } catch (Exception e) {
                sendError("Failed to retrieve deaths data: " + e.getMessage());
                LOGGER.error("Failed to retrieve deaths data", e);
                return null;
            }
        }

        private List<StatsTracker.PlayerPlaytime> fetchPlaytimes() {
            try {
                return StatsTracker.getOverallPlaytime(server);
            } catch (Exception e) {
                sendError("Failed to retrieve playtime data: " + e.getMessage());
                LOGGER.error("Failed to retrieve playtime data", e);
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

        private final List<StatsTracker.PlayerDeaths> deaths;
        private final Set<String> blacklistedPlayers;
        private final Map<String, ChatFormatting> usernameColors;
        private final DailyStatsTracker dailyStatsTracker;
        private final Map<UUID, Double> playtimeMap;

        LeaderboardFormatter(
                List<StatsTracker.PlayerDeaths> deaths,
                Set<String> blacklistedPlayers,
                Map<String, ChatFormatting> usernameColors,
                DailyStatsTracker dailyStatsTracker,
                Map<UUID, Double> playtimeMap
        ) {
            this.deaths = deaths;
            this.blacklistedPlayers = blacklistedPlayers;
            this.usernameColors = usernameColors;
            this.dailyStatsTracker = dailyStatsTracker;
            this.playtimeMap = playtimeMap;
        }

        void displayLeaderboard(CommandSourceStack source) {
            // Filter out blacklisted players
            List<StatsTracker.PlayerDeaths> filteredDeaths = deaths.stream()
                    .filter(pt -> !blacklistedPlayers.contains(pt.username()))
                    .toList();

            if (filteredDeaths.isEmpty()) {
                source.sendSystemMessage(Component.literal("No eligible players to display (all blacklisted or no data)")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            int maxUsernameLength = calculateMaxUsernameLength(filteredDeaths);
            int totalPadding = Math.max(BASE_PADDING, maxUsernameLength);

            String border = "=".repeat(42);
            MutableComponent borderComponent = Component.literal(border)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true));

            source.sendSystemMessage(borderComponent);
            source.sendSystemMessage(Component.literal("Deaths:")
                    .withStyle(ChatFormatting.DARK_AQUA));

            for (int i = 0; i < filteredDeaths.size(); i++) {
                formatPlayerEntry(source, filteredDeaths.get(i), i + 1, totalPadding);
                if (i < 2 && filteredDeaths.size() > 3) {
                    source.sendSystemMessage(Component.literal(""));
                }
            }

            source.sendSystemMessage(borderComponent);
        }

        private int calculateMaxUsernameLength(List<StatsTracker.PlayerDeaths> deaths) {
            return deaths.stream()
                    .map(pt -> pt.username().length())
                    .max(Integer::compareTo)
                    .orElse(0);
        }

        private void formatPlayerEntry(CommandSourceStack source, StatsTracker.PlayerDeaths pt, int position, int totalPadding) {
            PodiumRank rank = PodiumRank.fromPosition(position);
            MutableComponent message = rank.formatRank();
            if (rank != PodiumRank.NONE) {
                message = message.append(Component.literal(" "));
            }

            // Username
            String username = pt.username();
            String paddedUsername = username + ": " + " ".repeat(Math.max(0, totalPadding - username.length()));
            ChatFormatting usernameColor = usernameColors.getOrDefault(pt.username(), ChatFormatting.WHITE);
            MutableComponent usernameComponent = Component.literal(paddedUsername)
                    .withStyle(Style.EMPTY.withColor(usernameColor).withBold(false));
            message = message.append(usernameComponent);

            // Deaths
            String deathSingularPlural = (pt.deaths() == 1) ? "Death" : "Deaths";
            String deathsText = String.format("%d %s", pt.deaths(), deathSingularPlural);
            MutableComponent deathsComponent = Component.literal(deathsText)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false));

            // Playtime/Death Ratio
            double playtime = playtimeMap.getOrDefault(pt.uuid(), 0.0);

            double ratio;
            if (playtime > 0.0 && pt.deaths() >= 1)
            {
                ratio =  playtime / pt.deaths();
            } else {
                ratio = 0.0;
            }

            String ratioText = String.format("Pt/Death-Ratio: %.1f", ratio);
            int ratioIndex = ratioText.indexOf(":") + 2;
            MutableComponent ratioComponent = Component.literal(ratioText.substring(0, ratioIndex))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false));
            ratioComponent.append(Component.literal(ratioText.substring(ratioIndex))
                    .withStyle(Style.EMPTY.withColor(rank.color).withBold(false)));

            // Daily Deaths Hover
            String hoverText;
            if (dailyStatsTracker != null) {
                hoverText = DailyStatsTracker.formatDailyDeaths(pt.uuid());
            } else {
                hoverText = "N/A";
            }

            deathsComponent = deathsComponent.withStyle(deathsComponent.getStyle().withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
            ));

            message = message.append(deathsComponent).append(Component.literal("  ")).append(ratioComponent);

            source.sendSystemMessage(message);
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
