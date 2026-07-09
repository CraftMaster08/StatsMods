package net.craftmaster08.cm08statscore;

import net.craftmaster08.cm08statscore.ranking.RankEntry;
import net.minecraft.stats.Stat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StatProvider {
    long getTotal(UUID uuid, Collection<? extends Stat<?>> stats);
    long getDaily(UUID uuid, Collection<? extends Stat<?>> stats);
    Map<Stat<?>, Long> getTotals(UUID uuid, Collection<? extends Stat<?>> stats);
    Map<UUID, Long> getAllTotals(Collection<? extends Stat<?>> stats);
    List<RankEntry> getLeaderboard(Collection<? extends Stat<?>> stats);

    default long getTotal(UUID uuid, Stat<?> stat) {
        return getTotal(uuid, List.of(stat));
    }

    default long getDaily(UUID uuid, Stat<?> stat) {
        return getDaily(uuid, List.of(stat));
    }

    default List<RankEntry> getLeaderboard(Stat<?> stat) {
        return getLeaderboard(List.of(stat));
    }
}
