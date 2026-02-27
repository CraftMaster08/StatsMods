package net.craftmaster08.cm08statscore.statstracker;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.data.DataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DailyStatsTracker {
    private static final Logger LOGGER = LogManager.getLogger(DailyStatsTracker.class);

    private final String dailyKey;
    private final String lastKnownKey;
    private final String oldFileNameForMigration;
    private final double legacyConversionFactor;
    private final Path sharedDataPath;
    private final ResetScheduler resetScheduler;
    private final Map<UUID, Double> dailyStat = new HashMap<>();
    private final Map<UUID, Long> statLastKnownValue = new HashMap<>();
    private final StatsTracker statsTracker;
    private final Stat<ResourceLocation> liveStat;
    private final List<Stat<ResourceLocation>> liveStats;

    public DailyStatsTracker(String dailyKey, String lastKnownKey, String oldFileNameForMigration,
                             double legacyConversionFactor, StatsTracker statsTracker,
                             Stat<ResourceLocation> liveStat) {
        this.dailyKey = dailyKey;
        this.lastKnownKey = lastKnownKey;
        this.oldFileNameForMigration = oldFileNameForMigration;
        this.legacyConversionFactor = legacyConversionFactor;
        this.sharedDataPath = StatsCore.getDailyStatsPath();
        this.resetScheduler = new ResetScheduler(this);
        this.statsTracker = statsTracker;
        this.liveStat = liveStat;
        this.liveStats = null;
        loadData();
    }

    public DailyStatsTracker(String dailyKey, String lastKnownKey, String oldFileNameForMigration,
                             double legacyConversionFactor, StatsTracker statsTracker,
                             List<Stat<ResourceLocation>> liveStats) {
        this.dailyKey = dailyKey;
        this.lastKnownKey = lastKnownKey;
        this.oldFileNameForMigration = oldFileNameForMigration;
        this.legacyConversionFactor = legacyConversionFactor;
        this.sharedDataPath = StatsCore.getDailyStatsPath();
        this.resetScheduler = new ResetScheduler(this);
        this.statsTracker = statsTracker;
        this.liveStat = null;
        this.liveStats = liveStats;
        loadData();
    }

    private void loadData() {
        if (sharedDataPath == null) {
            LOGGER.error("Shared daily path null");
            resetScheduler.setLastResetCheck(Instant.now());
            return;
        }
        DataSerializer.load(sharedDataPath, dailyKey, lastKnownKey, dailyStat, resetScheduler,
                statLastKnownValue, oldFileNameForMigration, legacyConversionFactor);
    }

    void saveData() {
        if (sharedDataPath == null) return;
        DataSerializer.save(sharedDataPath, dailyKey, lastKnownKey, dailyStat,
                resetScheduler.getLastResetCheck(), statLastKnownValue);
    }

    public void setDailyResetTime(String timeStr) {
        resetScheduler.setDailyResetTime(timeStr);
    }

    public double getDailyStat(UUID uuid) {
        return dailyStat.getOrDefault(uuid, 0.0);
    }

    public ResetScheduler getResetScheduler() {
        return resetScheduler;
    }

    public boolean hasAnyNonZeroDailyStats() {
        return dailyStat.values().stream().anyMatch(v -> v > 0.0);
    }

    public void updatePlayerStat(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long currentStat = getCurrentStat(uuid);
        boolean wasNew = !statLastKnownValue.containsKey(uuid);
        long last = statLastKnownValue.getOrDefault(uuid, currentStat);
        double delta = currentStat - last;
        boolean changed = false;

        if (delta > 0) {
            dailyStat.merge(uuid, delta, Double::sum);
            changed = true;
        }
        statLastKnownValue.put(uuid, currentStat);

        if (wasNew || changed) {
            saveData();
        }
    }

    private long getCurrentStat(UUID uuid) {
        ServerPlayer p = StatsCore.getPlayerList().getPlayer(uuid);
        if (p != null) {
            if (liveStat != null) return p.getStats().getValue(liveStat);
            if (liveStats != null && !liveStats.isEmpty()) {
                return liveStats.stream().mapToLong(s -> p.getStats().getValue(s)).sum();
            }
        }
        StatsTracker.StatsEntry e = statsTracker.getStatByUUID(uuid);
        return e != null ? (long) e.stat() : 0L;
    }

    public void resetDailyStats() {
        dailyStat.replaceAll((u, v) -> 0.0);
    }
}