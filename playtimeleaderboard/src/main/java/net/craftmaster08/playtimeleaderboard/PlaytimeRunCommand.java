package net.craftmaster08.playtimeleaderboard;

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

        LeaderboardExecutor(CommandSourceStack source) {
            this.source = source;
            this.server = PlaytimeLeaderboard.getServer();
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
                LOGGER.warn("DailyPlaytimeTracker unavailable; daily playtime hover text disabled");
            }

            List<StatsTracker.PlayerPlaytime> playtimes = fetchPlaytimes();
            if (playtimes == null) {
                return 0;
            }
            if (playtimes.isEmpty()) {
                source.sendSystemMessage(Component.literal("No playtime data available")
                        .withStyle(ChatFormatting.YELLOW));
                return 1;
            }

            LeaderboardFormatter formatter = new LeaderboardFormatter(
                    StatsTracker.asStatsList(playtimes),
                    LeaderboardFormatter.StatsType.PLAYTIME,
                    config.getBlacklistedPlayers(),
                    config.getUsernameColors(),
                    dailyStatsTracker,
                    null
            );
            formatter.displayLeaderboard(source, ChatFormatting.GOLD, ChatFormatting.DARK_GREEN, LeaderboardFormatter.StatsType.PLAYTIME);
            return 1;
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