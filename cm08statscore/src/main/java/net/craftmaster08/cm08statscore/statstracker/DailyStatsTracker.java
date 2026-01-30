package net.craftmaster08.cm08statscore.statstracker;

import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.craftmaster08.cm08statscore.data.DataSerializer;

public class DailyStatsTracker {
    private final String statName;
    private final Path statDataPath;
    private final ResetScheduler resetScheduler;
    static final Map<UUID, Double> dailyStat = new HashMap<>();
    private final Map<UUID, Long> statLastKnownValue = new HashMap<>();
    private final StatsTracker statsTracker;

    public DailyStatsTracker(Path statDataPath, String statName, StatsTracker statsTracker) {
        this.statName = statName;
        this.statDataPath = statDataPath;
        this.resetScheduler = new ResetScheduler(this);
        this.statsTracker = statsTracker;
        setDailyResetTime("00:00:00 UTC");
        loadData();
    }

    private void loadData() {
        DataSerializer.load(statDataPath, dailyStat, resetScheduler, statName);
    }

    void saveData() {
        DataSerializer.save(statDataPath, dailyStat, resetScheduler.getLastResetCheck(), statName);
    }

    public void setDailyResetTime(String timeStr) {
        resetScheduler.setDailyResetTime(timeStr);
    }

    public double getDailyStat(UUID uuid) {
        return dailyStat.getOrDefault(uuid, 0.0);
    }

    public void updatePlayerStat(ServerPlayer player) {
        UUID uuid = player.getUUID();

        long currentStat = getCurrentStat(uuid);
        long lastStat = statLastKnownValue.getOrDefault(uuid, currentStat);
        double stat = currentStat - lastStat;

        dailyStat.merge(uuid, stat, Double::sum);
        statLastKnownValue.put(uuid, currentStat);
        resetScheduler.checkReset();
        saveData();
    }

    private long getCurrentStat(UUID uuid) {
        return (long) Objects.requireNonNull(statsTracker.getStatByUUID(uuid)).stat();
    }
}
