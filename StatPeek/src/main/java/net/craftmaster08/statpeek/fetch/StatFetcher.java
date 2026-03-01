package net.craftmaster08.statpeek.fetch;

import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.world.entity.player.Player;

public class StatFetcher {

    public Double getPlaytimeAsDouble(StatsTracker tracker, Player player) {
        StatsTracker.StatsEntry entry = tracker.getStatByUUID(player.getUUID());

        return entry.stat();
    }
}
