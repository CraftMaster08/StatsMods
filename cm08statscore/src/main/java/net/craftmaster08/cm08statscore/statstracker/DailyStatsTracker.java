package net.craftmaster08.cm08statscore.statstracker;

import net.craftmaster08.cm08statscore.StatsCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.craftmaster08.cm08statscore.data.DataSerializer;
import net.minecraft.stats.Stat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DailyStatsTracker {
    private static final Logger LOGGER = LogManager.getLogger(DailyStatsTracker.class);

    private final String statName;
    private final Path statDataPath;
    private final ResetScheduler resetScheduler;
    private final Map<UUID, Double> dailyStat = new HashMap<>();
    private final Map<UUID, Long> statLastKnownValue;
    private final StatsTracker statsTracker;
    private final Stat<ResourceLocation> liveStat;
    private final List<Stat<ResourceLocation>> liveStats;
    private final double legacyConversionFactor;


    public DailyStatsTracker(Path statDataPath, String statName, StatsTracker statsTracker, Stat<ResourceLocation> liveStat, double legacyConversionFactor) {
        this.statName = statName;
        this.statDataPath = statDataPath;
        this.resetScheduler = new ResetScheduler(this);
        this.statsTracker = statsTracker;
        this.liveStat = liveStat;
        this.liveStats = null;
        this.statLastKnownValue = new HashMap<>();
        this.legacyConversionFactor = legacyConversionFactor;
        loadData();
    }

    public DailyStatsTracker(Path statDataPath, String statName, StatsTracker statsTracker, List<Stat<ResourceLocation>> liveStats, double legacyConversionFactor) {
        this.statName = statName;
        this.statDataPath = statDataPath;
        this.resetScheduler = new ResetScheduler(this);
        this.statsTracker = statsTracker;
        this.liveStat = null;
        this.liveStats = liveStats;
        this.statLastKnownValue = new HashMap<>();
        this.legacyConversionFactor = legacyConversionFactor;
        loadData();
    }

    private void loadData() {
        DataSerializer.load(statDataPath, dailyStat, resetScheduler, statName, statLastKnownValue);

        if (statLastKnownValue.isEmpty() && !dailyStat.isEmpty()) {
            LOGGER.info("Legacy daily stats detected for {}. Converting with factor {}", statName, legacyConversionFactor);
            dailyStat.replaceAll(((uuid, val) -> val * legacyConversionFactor));
        }
    }

    void saveData() {
        DataSerializer.save(statDataPath, dailyStat, resetScheduler.getLastResetCheck(), statName, statLastKnownValue);
    }

    public void setDailyResetTime(String timeStr) {
        resetScheduler.setDailyResetTime(timeStr);
    }

    public double getDailyStat(UUID uuid) {
        return dailyStat.getOrDefault(uuid, 0.0);
    }

    public void updatePlayerStat(ServerPlayer player) {
        resetScheduler.checkReset();

        UUID uuid = player.getUUID();
        long currentStat = getCurrentStat(uuid);

        long lastStat = statLastKnownValue.getOrDefault(uuid, currentStat);
        double delta = currentStat - lastStat;

        dailyStat.merge(uuid, delta, Double::sum);
        statLastKnownValue.put(uuid, currentStat);
        saveData();
    }

    private long getCurrentStat(UUID uuid) {
        ServerPlayer player = StatsCore.getPlayerList().getPlayer(uuid);
        if (player != null) {
            if (liveStat != null) {
                return player.getStats().getValue(liveStat);
            }
            if (liveStats != null && !liveStats.isEmpty()) {
                return liveStats.stream().mapToLong(s ->
                        player.getStats().getValue(s)).sum();
            }
        }

        StatsTracker.StatsEntry entry = statsTracker.getStatByUUID(uuid);
        return entry != null ? (long) entry.stat() : 0L;
    }

    public void resetDailyStats() {
        dailyStat.replaceAll((uuid, v) -> 0.0);
    }
}
