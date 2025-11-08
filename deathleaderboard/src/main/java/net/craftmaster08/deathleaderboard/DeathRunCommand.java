package net.craftmaster08.deathleaderboard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.ranking.LeaderboardFormatter;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
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
                    StatsTracker.asStatsList(deaths),
                    LeaderboardFormatter.StatsType.DEATHS,
                    config.getBlacklistedPlayers(),
                    config.getUsernameColors(),
                    dailyStatsTracker,
                    playtimeMap
            );
            formatter.displayLeaderboard(source, ChatFormatting.DARK_RED, ChatFormatting.DARK_AQUA, LeaderboardFormatter.StatsType.DEATHS);
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
}
