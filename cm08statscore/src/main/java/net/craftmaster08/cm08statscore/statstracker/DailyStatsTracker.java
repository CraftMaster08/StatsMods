package net.craftmaster08.cm08statscore.statstracker;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.data.DataSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import java.nio.file.Path;
import java.util.*;

public class DailyStatsTracker {

    private final Set<Stat<?>> trackedStats;
    private final String dailyKey;
    private final String lastKnownKey;
    private final double legacyConversionFactor;
    private final Map<UUID, Double> dailyStat = new HashMap<>();
    private final Map<UUID, Long> lastKnownValue = new HashMap<>();
    private final ResetScheduler resetScheduler;

    public DailyStatsTracker(
            Collection<? extends Stat<?>> stats,
            String dailyKey,
            String lastKnownKey,
            double legacyConversionFactor) {
        this(stats, dailyKey, lastKnownKey, legacyConversionFactor, null);
    }

    public DailyStatsTracker(
            Collection<? extends Stat<?>> stats,
            String dailyKey,
            String lastKnownKey,
            double legacyConversionFactor,
            String oldFileName) {
        this.trackedStats = new LinkedHashSet<>(stats);
        this.dailyKey = dailyKey;
        this.lastKnownKey = lastKnownKey;
        this.legacyConversionFactor = legacyConversionFactor;
        this.resetScheduler = new ResetScheduler(this);
        loadData(oldFileName);
    }

    public boolean tracks(Collection<? extends Stat<?>> stats) {
        return trackedStats.containsAll(stats);
    }

    public void updatePlayerStat(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long current = 0L;
        for (Stat<?> stat : trackedStats) {
            current += player.getStats().getValue(stat);
        }
        long prev = lastKnownValue.getOrDefault(uuid, current);
        double delta = (current - prev) / legacyConversionFactor;

        if (delta > 0) {
            dailyStat.merge(uuid, delta, Double::sum);
            saveData();
        }
        lastKnownValue.put(uuid, current);
    }

    public double getDaily(UUID uuid) {
        return dailyStat.getOrDefault(uuid, 0.0);
    }

    public void incrementDaily(UUID uuid, double amount) {
        dailyStat.merge(uuid, amount, Double::sum);
        saveData();
    }

    public void resetDailyStats() {
        dailyStat.replaceAll((u, v) -> 0.0);
        saveData();
    }

    public ResetScheduler getResetScheduler() {
        return resetScheduler;
    }

    public void setDailyResetTime(String timeStr) {
        resetScheduler.setDailyResetTime(timeStr);
    }

    public boolean hasAnyNonZeroDailyStats() {
        return dailyStat.values().stream().anyMatch(v -> v > 0.0);
    }

    private void loadData(String oldFileName) {
        Path path = StatsCore.getDailyStatsPath();
        if (path == null) return;
        DataSerializer.load(path, dailyKey, lastKnownKey, dailyStat, resetScheduler,
                lastKnownValue, oldFileName, legacyConversionFactor);
    }

    public void saveData() {
        Path path = StatsCore.getDailyStatsPath();
        if (path == null) return;
        DataSerializer.save(path, dailyKey, lastKnownKey, dailyStat,
                resetScheduler.getLastResetCheck(), lastKnownValue);
    }
}
