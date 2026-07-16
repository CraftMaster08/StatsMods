package net.craftmaster08.deathleaderboard;

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
import net.minecraft.stats.Stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        List<RankEntry> deaths = StatsCore.getProvider().getLeaderboard(Stats.CUSTOM.get(Stats.DEATHS));
        if (deaths.isEmpty()) {
            source.sendSystemMessage(Component.literal("No deaths data available")
                    .withStyle(ChatFormatting.YELLOW));
            return 1;
        }

        Map<UUID, Double> playtimeHours = fetchPlaytimeHours();

        LeaderboardFormatter formatter = new LeaderboardFormatter(
                deaths,
                config.getBlacklistedPlayers(),
                config.getUsernameColors()
        );
        formatter.displayLeaderboard(source, ChatFormatting.BLACK, "Deaths: ", ChatFormatting.DARK_AQUA, "/deaths", page,
                (entry, position) -> formatDeathStat(entry, position, playtimeHours));
        return 1;
    }

    private Map<UUID, Double> fetchPlaytimeHours() {
        var playTimeStat = Stats.CUSTOM.get(Stats.PLAY_TIME);
        Map<UUID, Long> ticks = StatsCore.getProvider().getAllTotals(List.of(playTimeStat));
        Map<UUID, Double> hours = new HashMap<>();
        ticks.forEach((uuid, t) -> hours.put(uuid, t / 72000.0));
        return hours;
    }

    private MutableComponent formatDeathStat(RankEntry entry, int position, Map<UUID, Double> playtimeHours) {
        String singularPlural = entry.value() == 1 ? "Death" : "Deaths";

        double playtime = playtimeHours.getOrDefault(entry.uuid(), 0.0);
        double ratio = (playtime > 0.0 && entry.value() >= 1) ? playtime / entry.value() : playtime;

        var dailyTracker = DeathLeaderboard.getDailyTracker();
        String hoverText = dailyTracker != null
                ? formatDailyDeaths((int) dailyTracker.getDaily(entry.uuid()))
                : "N/A";

        MutableComponent baseComponent = Component.literal(String.format("%d %s", entry.value(), singularPlural))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))
                .withStyle(s -> s.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText))
                ));

        PodiumRank rank = PodiumRank.fromPosition(position);

        String ratioText = String.format("PT/Death-Ratio: %.1f", ratio);
        int ratioIndex = ratioText.indexOf(":") + 2;
        MutableComponent ratioComponent = Component.literal(ratioText.substring(0, ratioIndex))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false));
        ratioComponent.append(Component.literal(ratioText.substring(ratioIndex))
                .withStyle(Style.EMPTY.withColor(rank.getColor()).withBold(false)));

        MutableComponent valueText = Component.literal("");
        valueText = valueText.append(baseComponent).append(Component.literal("    ")).append(ratioComponent);
        return valueText;
    }

    public static String formatDailyDeaths(int dailyDeaths) {
        String singularPlural = dailyDeaths == 1 ? "Death" : "Deaths";
        return String.format("%d %s today", dailyDeaths, singularPlural);
    }

    private void sendError(String message) {
        source.sendSystemMessage(Component.literal(message)
                .withStyle(ChatFormatting.RED));
    }
}
