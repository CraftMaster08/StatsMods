package net.craftmaster08.killleaderboard;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.ranking.LeaderboardFormatter;
import net.craftmaster08.cm08statscore.ranking.RankEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.List;

public class LeaderboardExecutor {
    private final CommandSourceStack source;
    private final int page;

    public LeaderboardExecutor(CommandSourceStack source, int page) {
        this.source = source;
        this.page = page;
    }

    public int execute() {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(Component.literal("Must be run by player").withStyle(ChatFormatting.RED));
            return 0;
        }

        ConfigManager config = StatsCore.getConfigManager();
        if (config == null) {
            sendError("StatsCore configuration not initialized");
            return 0;
        }

        if (!StatsCore.canUseCommand(player.getUUID())) {
            source.sendSystemMessage(Component.literal("Please wait " + config.cooldownSeconds + "s before using this command again.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        List<RankEntry> kills = StatsCore.getProvider().getLeaderboard(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
        if (kills.isEmpty()) {
            source.sendSystemMessage(Component.literal("No kills data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                kills,
                config.getBlacklistedPlayers(),
                config.getUsernameColors()
        );
        formatter.displayLeaderboard(source, ChatFormatting.DARK_RED, "Kills: ", ChatFormatting.YELLOW, "/playerkills", page, this::formatKillStat);
        return 1;
    }

    private MutableComponent formatKillStat(RankEntry entry, int position) {
        String singularPlural = entry.value() == 1 ? "Kill" : "Kills";
        String hoverText;

        var dailyTracker = KillLeaderboard.getDailyTracker();
        if (dailyTracker != null) {
            hoverText = formatDailyKills((int) dailyTracker.getDaily(entry.uuid()));
        } else {
            hoverText = "N/A";
        }

        return Component.literal(String.format("%d %s", entry.value(), singularPlural))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));
    }

    public static String formatDailyKills(int dailyKills) {
        String singularPlural = dailyKills == 1 ? "Kill" : "Kills";
        return String.format("%d %s today", dailyKills, singularPlural);
    }

    private void sendError(String message) {
        source.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.RED));
    }
}
