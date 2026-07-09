// LeaderboardExecutor.java (full updated version)
package net.craftmaster08.playtimeleaderboard;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.ranking.LeaderboardFormatter;
import net.craftmaster08.cm08statscore.ranking.PodiumRank;
import net.craftmaster08.cm08statscore.ranking.RankEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

public class LeaderboardExecutor {
    private final CommandSourceStack source;
    private final Logger logger = LogManager.getLogger(LeaderboardExecutor.class);

    public LeaderboardExecutor(CommandSourceStack source) {
        this.source = source;
    }

    public int execute() {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(Component.literal("Must be run by player").withStyle(ChatFormatting.RED));
            return 0;
        }

        ConfigManager cfg = StatsCore.getConfigManager();
        if (cfg == null) {
            source.sendSystemMessage(Component.literal("Config not loaded").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!StatsCore.canUseCommand(player.getUUID())) {
            source.sendSystemMessage(
                    Component.literal("Wait " + cfg.cooldownSeconds + "s").withStyle(ChatFormatting.RED));
            return 0;
        }

        List<RankEntry> entries = fetchPlaytimeData();
        if (entries.isEmpty()) {
            source.sendSystemMessage(Component.literal("No data").withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                entries,
                cfg.getBlacklistedPlayers(),
                cfg.getUsernameColors()
        );

        formatter.displayLeaderboard(
                source,
                ChatFormatting.GOLD,
                "Playtime",
                ChatFormatting.DARK_GREEN,
                this::formatPlaytimeStat
        );

        return 1;
    }

    private List<RankEntry> fetchPlaytimeData() {
        var provider = StatsCore.getProvider();
        var playTimeStat = net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME);
        return provider.getLeaderboard(playTimeStat);
    }

    private MutableComponent formatPlaytimeStat(RankEntry entry, int position) {
        double hours = entry.value() / 72000.0; // value() holds raw playtime ticks
        var range = HourRange.findRange(hours);

        MutableComponent text = range.formatHours(hours);

        var dailyTracker = PlaytimeLeaderboard.getDailyTracker();
        if (dailyTracker != null) {
            double dailyHours = dailyTracker.getDaily(entry.uuid());
            String hover = formatDailyPlaytime(dailyHours);
            text = text.withStyle(s -> s.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
        }

        if (hours >= 100) {
            double days = hours / 24.0;
            text = text.append(Component.literal(String.format("  (%.2fd)", days))
                    .withStyle(Style.EMPTY.withColor(PodiumRank.fromPosition(position).getColor()).withBold(true)));
        }

        return text;
    }

    private static String formatDailyPlaytime(double hours) {
        double totalSec = hours * 3600;
        int h = (int) (totalSec / 3600);
        int m = (int) ((totalSec % 3600) / 60);
        int s = (int) (totalSec % 60);
        return String.format("%dh %dmin %dsec today", h, m, s);
    }
}