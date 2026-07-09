package net.craftmaster08.distanceleaderboard;

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
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaderboardExecutor {
    private static final Logger LOGGER = LogManager.getLogger(LeaderboardExecutor.class);
    private final CommandSourceStack source;

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

        List<RankEntry> distances = fetchDistances(config);
        if (distances.isEmpty()) {
            source.sendSystemMessage(Component.literal("No distance data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                distances,
                config.getBlacklistedPlayers(),
                config.getUsernameColors()
        );
        formatter.displayLeaderboard(source, ChatFormatting.DARK_AQUA, "Distance: ", ChatFormatting.GOLD, this::formatDistanceStat);
        return 1;
    }

    private List<RankEntry> fetchDistances(ConfigManager config) {
        List<RankEntry> entries = StatsCore.getProvider().getLeaderboard(DistanceLeaderboard.DISTANCE_STATS);

        Map<String, Integer> intLimits = config.getIntLimits();
        if (intLimits.isEmpty()) {
            return entries;
        }

        List<RankEntry> boosted = new ArrayList<>(entries.size());
        for (RankEntry e : entries) {
            Integer n = intLimits.get(e.username());
            long value = (n == null || n <= 0) ? e.value() : e.value() + (long) n * 2147483647L;
            boosted.add(new RankEntry(e.uuid(), e.username(), value, e.rank()));
        }

        boosted.sort((a, b) -> {
            int cmp = Long.compare(b.value(), a.value());
            return cmp != 0 ? cmp : a.username().compareToIgnoreCase(b.username());
        });

        List<RankEntry> ranked = new ArrayList<>(boosted.size());
        for (int i = 0; i < boosted.size(); i++) {
            RankEntry e = boosted.get(i);
            ranked.add(new RankEntry(e.uuid(), e.username(), e.value(), i + 1));
        }
        return ranked;
    }

    private MutableComponent formatDistanceStat(RankEntry entry, int position) {
        var dailyTracker = DistanceLeaderboard.getDailyTracker();
        String hoverText = dailyTracker != null
                ? formatDailyDistance(dailyTracker.getDaily(entry.uuid()))
                : "N/A";

        double distanceKM = entry.value() / 100000.0;

        return Component.literal(formatDistance(distanceKM))
                .withStyle(ChatFormatting.WHITE)
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));
    }

    public static String formatDailyDistance(double distanceCm) {
        double totalMeters = distanceCm / 100.0;
        int km = (int) (totalMeters / 1000);
        int m = (int) (totalMeters % 1000);
        return String.format("%dkm %dm today", km, m);
    }

    public static String formatDistance(double distanceKm) {
        if (distanceKm >= 1000.0) {
            return String.format("%dkm", (int) distanceKm);
        } else if (distanceKm >= 1.0) {
            return String.format("%.2fkm", distanceKm);
        } else {
            double meters = distanceKm * 1000.0;
            return String.format("%dm", (int) meters);
        }
    }

    private void sendError(String message) {
        source.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.RED));
    }
}
