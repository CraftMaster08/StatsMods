package net.craftmaster08.killleaderboard;

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

public class KillRunCommand {
    private static final Logger LOGGER = LogManager.getLogger(KillRunCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("kills")
                .requires(source -> source.hasPermission(0))
                .executes(context -> new LeaderboardExecutor(context.getSource()).execute());

        try {
            dispatcher.register(command);
            LOGGER.info("Successfully registered /kills command");
        } catch (Exception e) {
            LOGGER.error("Failed to register /kills command", e);
        }
    }

    private static class LeaderboardExecutor {
        private final CommandSourceStack source;
        private final MinecraftServer server;
        private final ConfigManager config;
        private final DailyStatsTracker dailyStatsTracker;

        LeaderboardExecutor(CommandSourceStack source) {
            this.source = source;
            this.server = KillLeaderboard.getServer();
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
                sendError("DailyStatsTracker unavailable; daily kills hover text disabled");
            }

            List<StatsTracker.StatsEntry> kills = fetchKills();
            if (kills == null) {
                return 0;
            }
            if (kills.isEmpty()) {
                source.sendSystemMessage(Component.literal("No kills data available")
                        .withStyle(ChatFormatting.YELLOW));
                return 1;
            }

            LeaderboardFormatter formatter = new LeaderboardFormatter(
                    kills,
                    LeaderboardFormatter.StatsType.KILLS,
                    config.getBlacklistedPlayers(),
                    config.getUsernameColors(),
                    dailyStatsTracker,
                    null
            );
            formatter.displayLeaderboard(source, ChatFormatting.DARK_RED, ChatFormatting.YELLOW, LeaderboardFormatter.StatsType.KILLS);
            return 1;
        }

        private List<StatsTracker.StatsEntry> fetchKills() {
            try {
                // refresh daily kills
                PlayerList serverPlayers = StatsCore.getPlayerList();
                for (ServerPlayer player : serverPlayers.getPlayers()) {
                    dailyStatsTracker.updatePlayerKills(player);
                }
                return StatsTracker.getOverallStats(server, LeaderboardFormatter.StatsType.KILLS);
            } catch (Exception e) {
                sendError("Failed to retrieve kills data: " + e.getMessage());
                LOGGER.error("Failed to retrieve kills data", e);
                return null;
            }
        }

        private void sendError(String message) {
            source.sendSystemMessage(Component.literal(message)
                    .withStyle(ChatFormatting.RED));
        }
    }
}