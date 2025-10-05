package net.craftmaster08.cm08statscore.statstracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.craftmaster08.cm08statscore.statstracker.StatsTracker.calculatePlayerDistance;

public class DailyStatsTracker {
    private static final Logger LOGGER = LogManager.getLogger(DailyStatsTracker.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path distanceDataPath;
    private final Path playtimeDataPath;
    private final MinecraftServer server;
    private final ResetScheduler resetScheduler;
    private final Map<UUID, Double> dailyPlaytimes;
    private final Map<UUID, Double> dailyDistances;
    private final Map<UUID, Long> playtimeLastKnownTicks;
    private final Map<UUID, Long> distanceLastKnownTicks;
    private final Stat<?> playTimeStat;

    public DailyStatsTracker(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("MinecraftServer cannot be null");
        }
        this.server = server;
        this.distanceDataPath = server.getWorldPath(LevelResource.ROOT).resolve("distance_daily.json");
        this.playtimeDataPath = server.getWorldPath(LevelResource.ROOT).resolve("playtime_daily.json");
        this.dailyPlaytimes = new HashMap<>();
        this.dailyDistances = new HashMap<>();
        this.playtimeLastKnownTicks = new HashMap<>();
        this.distanceLastKnownTicks = new HashMap<>();
        this.resetScheduler = new DailyStatsTracker.ResetScheduler(this);
        try {
            this.playTimeStat = Stats.CUSTOM.get(Stats.PLAY_TIME);
            if (this.playTimeStat == null) {
                throw new IllegalStateException("Stats.PLAY_TIME not found in Stats.CUSTOM");
            }
            //LOGGER.info("Successfully accessed Stats.PLAY_TIME");
        } catch (Exception e) {
            LOGGER.error("Failed to access Stats.PLAY_TIME", e);
            throw new RuntimeException("Cannot initialize DailyPlaytimeTracker without Stats.PLAY_TIME", e);
        }
        setDailyResetTime("00:00:00 UTC"); // Default reset time, can be set externally
        loadData();
    }

    private void loadData() {
        DataSerializer.load(playtimeDataPath, dailyPlaytimes, resetScheduler, "daily_playtimes");
        DataSerializer.load(distanceDataPath, dailyDistances, resetScheduler, "daily_distances");
    }

    private void saveData() {
        DataSerializer.save(playtimeDataPath, dailyPlaytimes, resetScheduler.getLastResetCheck(), "daily_playtimes");
        DataSerializer.save(distanceDataPath, dailyDistances, resetScheduler.getLastResetCheck(), "daily_distances");
    }

    public void setDailyResetTime(String timeStr) {
        resetScheduler.setDailyResetTime(timeStr);
    }

    public double getDailyPlaytime(UUID uuid) {
        return dailyPlaytimes.getOrDefault(uuid, 0.0);
    }

    public double getDailyDistance(UUID uuid) {
        return dailyDistances.getOrDefault(uuid, 0.0);
    }

    public void updatePlayerDistance(ServerPlayer player) {
        UUID uuid = player.getUUID();
        double currentDistanceCm = calculatePlayerDistance(player);
        double lastDistanceCm = distanceLastKnownTicks.getOrDefault(uuid, (long) currentDistanceCm).doubleValue();

        double distanceTraveledCm = currentDistanceCm - lastDistanceCm;
        dailyDistances.merge(uuid, distanceTraveledCm, Double::sum);
        distanceLastKnownTicks.put(uuid, (long) currentDistanceCm);

        resetScheduler.checkReset();
    }

    public void updatePlayerPlaytime(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long currentTicks = player.getStats().getValue(playTimeStat);
        long lastTicks = playtimeLastKnownTicks.getOrDefault(uuid, currentTicks);

        double hoursPlayed = (currentTicks - lastTicks) / 20.0 / 3600.0;
        dailyPlaytimes.merge(uuid, hoursPlayed, Double::sum);
        playtimeLastKnownTicks.put(uuid, currentTicks);

        resetScheduler.checkReset();
    }

    public void playerLoggedIn(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long currentTicks = player.getStats().getValue(playTimeStat);
        playtimeLastKnownTicks.put(uuid, currentTicks);
        dailyPlaytimes.putIfAbsent(uuid, 0.0);

        double currentDistanceCm = calculatePlayerDistance(player);
        distanceLastKnownTicks.put(uuid, (long) currentDistanceCm);
        dailyDistances.putIfAbsent(uuid, 0.0);
    }

    public void playerLoggedOut(ServerPlayer player) {
        updatePlayerDistance(player);
        updatePlayerPlaytime(player);
        saveData();
    }

    public static String formatDailyPlaytime(double hours) {
        double totalSecondsDouble = hours * 3600.0;
        int h = (int) (totalSecondsDouble / 3600);
        double remainingSeconds = totalSecondsDouble % 3600;
        int m = (int) (remainingSeconds / 60);
        int s = (int) (remainingSeconds % 60);
        return String.format("%dh %dmin %dsec today", h, m, s);
    }

    public static String formatDailyDistance(double distanceCm) {
        double totalMeters = distanceCm / 100.0;
        int km = (int) (totalMeters / 1000);
        int m = (int) (totalMeters % 1000);
        return String.format("%dkm %dm today", km, m);
    }

    private static class ResetScheduler {
        private String dailyResetTime;
        private LocalTime resetTime;
        private Instant lastResetCheck;
        private final DailyStatsTracker tracker;

        ResetScheduler(DailyStatsTracker tracker) {
            this.tracker = tracker;
            this.lastResetCheck = Instant.now();
        }

        void setDailyResetTime(String timeStr) {
            try {
                String[] parts = timeStr.split(" ");
                if (parts.length != 2 || !parts[1].equals("UTC")) {
                    throw new DateTimeParseException("Invalid format, expected 'HH:mm:ss UTC'", timeStr, 0);
                }
                this.resetTime = LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH:mm:ss"));
                String timeUTC = timeStr + " UTC";
                this.dailyResetTime = timeUTC;
                LOGGER.info("Set daily reset time to: {}", timeUTC);
            } catch (DateTimeParseException e) {
                LOGGER.error("Invalid daily_reset_time format: {}. Defaulting to 00:00:00 UTC", timeStr, e);
                this.resetTime = LocalTime.of(0, 0, 0);
                this.dailyResetTime = "00:00:00 UTC";
            }
        }

        void checkReset() {
            Instant now = Instant.now();
            ZonedDateTime currentZdt = ZonedDateTime.ofInstant(now, ZoneId.of("UTC"));
            ZonedDateTime lastCheckZdt = ZonedDateTime.ofInstant(lastResetCheck, ZoneId.of("UTC"));
            LocalDate today = currentZdt.toLocalDate();
            ZonedDateTime todayReset = ZonedDateTime.of(today, resetTime, ZoneId.of("UTC"));

            if (currentZdt.isAfter(todayReset) && lastCheckZdt.isBefore(todayReset)) {
                LOGGER.info("Resetting daily distance at {}", currentZdt);
                tracker.dailyDistances.replaceAll((uuid, v) -> 0.0);
                tracker.dailyPlaytimes.replaceAll((uuid, v) -> 0.0);
                /* more stats here */
                tracker.saveData();
            }

            if (!currentZdt.toLocalDate().equals(lastCheckZdt.toLocalDate())) {
                ZonedDateTime tomorrowReset = todayReset.plusDays(1);
                if (currentZdt.isAfter(tomorrowReset) && lastCheckZdt.isBefore(tomorrowReset)) {
                    LOGGER.info("Resetting daily distance at {}", currentZdt);
                    tracker.dailyDistances.replaceAll((uuid, v) -> 0.0);
                    tracker.dailyPlaytimes.replaceAll((uuid, v) -> 0.0);
                    /* more stats here */
                    tracker.saveData();
                }
            }

            lastResetCheck = now;
        }

        Instant getLastResetCheck() {
            return lastResetCheck;
        }
    }

    private static class DataSerializer {
        static void load(Path dataPath, Map<UUID, Double> dailyStats, DailyStatsTracker.ResetScheduler resetScheduler, String statsFile) {
            File dataFile = dataPath.toFile();
            if (!dataFile.exists()) {
                save(dataPath, dailyStats, resetScheduler.getLastResetCheck(), statsFile);
                return;
            }

            try (FileReader reader = new FileReader(dataFile)) {
                JsonObject dataJson = GSON.fromJson(reader, JsonObject.class);
                if (dataJson == null) {
                    throw new JsonParseException("Daily stats file is empty or contains invalid JSON");
                }

                if (dataJson.has(statsFile)) {
                    JsonObject statsJson = dataJson.getAsJsonObject(statsFile);
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : statsJson.entrySet()) {
                        try {
                            UUID uuid = UUID.fromString(entry.getKey());
                            dailyStats.put(uuid, entry.getValue().getAsDouble());
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid UUID in stats data: {}", entry.getKey());
                        }
                    }
                }

                if (dataJson.has("last_reset_check")) {
                    try {
                        resetScheduler.lastResetCheck = Instant.parse(dataJson.get("last_reset_check").getAsString());
                    } catch (DateTimeParseException e) {
                        LOGGER.warn("Invalid last_reset_check format, using current time");
                        resetScheduler.lastResetCheck = Instant.now();
                    }
                }

                LOGGER.info(String.format("Successfully loaded %s.json", dataFile));
            } catch (IOException | JsonParseException e) {
                LOGGER.error(String.format("Failed to load %s.json", dataFile), e);
                dailyStats.clear();
                resetScheduler.lastResetCheck = Instant.now();
            }
        }

        static void save(Path dataPath, Map<UUID, Double> dailyStats, Instant lastResetCheck, String dataFile) {
            JsonObject dataJson = new JsonObject();
            JsonObject statsJson = new JsonObject();
            dailyStats.forEach((uuid, hours) -> statsJson.addProperty(uuid.toString(), hours));
            dataJson.add(dataFile, statsJson);
            dataJson.addProperty("last_reset_check", lastResetCheck.toString());

            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(dataJson, writer);
                LOGGER.info(String.format("Saved %s.json", dataFile));
            } catch (IOException e) {
                LOGGER.error(String.format("Failed to save %s.json", dataFile), e);
            }
        }
    }
}
