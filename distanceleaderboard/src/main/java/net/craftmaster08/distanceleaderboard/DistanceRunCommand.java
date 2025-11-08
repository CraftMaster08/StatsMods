package net.craftmaster08.distanceleaderboard;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

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
                    StatsTracker.asStatsList(distances),
                    LeaderboardFormatter.StatsType.DISTANCE,
                    config.getBlacklistedPlayers(),
                    config.getUsernameColors(),
                    dailyStatsTracker,
                    null
            );
            formatter.displayLeaderboard(source, ChatFormatting.DARK_AQUA, ChatFormatting.GOLD, LeaderboardFormatter.StatsType.DISTANCE);
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
}