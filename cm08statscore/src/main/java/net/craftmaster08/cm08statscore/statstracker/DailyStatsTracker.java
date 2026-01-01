package net.craftmaster08.cm08statscore.statstracker;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.crypto.Data;
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


public class DailyStatsTracker {
    private static final Logger LOGGER = LogManager.getLogger(DailyStatsTracker.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final String statName;
    private final Path statDataPath;
    private final MinecraftServer server;
    private final ResetScheduler resetScheduler;
    private static final Map<UUID, Double> dailyStat = new HashMap<>();
    private final Map<UUID, Long> statLastKnownValue = new HashMap<>();
    private final Stat<?> stat;
    private final String resourceLocation;
    private final String statType;

    public DailyStatsTracker(MinecraftServer server, Path statDataPath, ResourceLocation type, String resourceLocation, String statType, String statName) {
        if (server == null) {
            throw new IllegalArgumentException("MinecraftServer cannot be null");
        }
        this.server = server;
        //this.distanceDataPath = server.getWorldPath(LevelResource.ROOT).resolve("distance_daily.json");
        //this.playtimeDataPath = server.getWorldPath(LevelResource.ROOT).resolve("playtime_daily.json");
        //this.deathsDataPath = server.getWorldPath(LevelResource.ROOT).resolve("deaths_daily.json");
        //this.killsDataPath = server.getWorldPath(LevelResource.ROOT).resolve("kills_daily.json");
        this.statName = statName;
        this.statDataPath = statDataPath;
        this.resetScheduler = new ResetScheduler(this);
        this.stat = Stats.CUSTOM.get(type);
        this.resourceLocation = resourceLocation;
        this.statType = statType;
        setDailyResetTime("00:00:00 UTC");
        loadData();
    }

    private void loadData() {
        DataSerializer.load(statDataPath, dailyStat, resetScheduler, statName);
        //DataSerializer.load(playtimeDataPath, dailyPlaytimes, resetScheduler, "daily_playtimes");
        //DataSerializer.load(distanceDataPath, dailyDistances, resetScheduler, "daily_distances");
        //DataSerializer.load(deathsDataPath, dailyDeaths, resetScheduler, "daily_deaths");
        //DataSerializer.load(killsDataPath, dailyKills, resetScheduler, "daily_kills");
    }

    private void saveData() {
        DataSerializer.save(statDataPath, dailyStat, resetScheduler.getLastResetCheck(), statName);
        //DataSerializer.save(playtimeDataPath, dailyPlaytimes, resetScheduler.getLastResetCheck(), "daily_playtimes");
        //DataSerializer.save(distanceDataPath, dailyDistances, resetScheduler.getLastResetCheck(), "daily_distances");
        //DataSerializer.save(deathsDataPath, dailyDeaths, resetScheduler.getLastResetCheck(), "daily_deaths");
        //DataSerializer.save(killsDataPath, dailyKills, resetScheduler.getLastResetCheck(), "daily_kills");
    }

    public void setDailyResetTime(String timeStr) {
        resetScheduler.setDailyResetTime(timeStr);
    }

    public double getDailyStat(UUID uuid)
    {
        return dailyStat.getOrDefault(uuid, 0.0);
    }

    public void updatePlayerStat(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long currentStat = (long) StatsTracker.getStatByUUID(server, statType, resourceLocation, uuid).stat();
        long lastStat = statLastKnownValue.getOrDefault(uuid, currentStat);

        double stat = currentStat - lastStat;
        dailyStat.merge(uuid, stat, Double::sum);
        statLastKnownValue.put(uuid, currentStat);

        resetScheduler.checkReset();
        saveData();
    }

    public void playerLoggedIn(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long currentStat = (long) StatsTracker.getStatByUUID(server, statType, resourceLocation, uuid).stat();
        statLastKnownValue.put(uuid, currentStat);
        dailyStat.putIfAbsent(uuid, 0.0);
    }



    public static String formatDailyDistance(double distanceCm) {
        double totalMeters = distanceCm / 100.0;
        int km = (int) (totalMeters / 1000);
        int m = (int) (totalMeters % 1000);
        return String.format("%dkm %dm today", km, m);
    }

    public static String formatDailyDeaths(UUID uuid) {
        int dailyDeaths = (int) getDailyDeaths(uuid);
        String singularPlural = dailyDeaths == 1 ? "Death" : "Deaths";

        return String.format("%d %s today", dailyDeaths, singularPlural);
    }

    public static String formatDailyKills(UUID uuid) {
        int dailyKills = (int) getDailyKills(uuid);
        String singularPlural = dailyKills == 1 ? "Kill" : "Kills";

        return String.format("%d %s today", dailyKills, singularPlural);
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
                LOGGER.info("Resetting daily stats at {}", currentZdt);
                tracker.dailyDistances.replaceAll((uuid, v) -> 0.0);
                tracker.dailyPlaytimes.replaceAll((uuid, v) -> 0.0);
                dailyDeaths.replaceAll((uuid, v) -> 0.0);
                dailyKills.replaceAll((uuid, aDouble) -> 0.0);

                /* more stats here */
                tracker.saveData();
            }

            if (!currentZdt.toLocalDate().equals(lastCheckZdt.toLocalDate())) {
                ZonedDateTime tomorrowReset = todayReset.plusDays(1);
                if (currentZdt.isAfter(tomorrowReset) && lastCheckZdt.isBefore(tomorrowReset)) {
                    LOGGER.info("Resetting daily stats at {}", currentZdt);
                    tracker.dailyDistances.replaceAll((uuid, v) -> 0.0);
                    tracker.dailyPlaytimes.replaceAll((uuid, v) -> 0.0);
                    dailyDeaths.replaceAll((uuid, v) -> 0.0);
                    dailyKills.replaceAll((uuid, aDouble) -> 0.0);

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
        static void load(Path dataPath, Map<UUID, Double> dailyStats, ResetScheduler resetScheduler, String statsFile) {
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
                    for (Map.Entry<String, JsonElement> entry : statsJson.entrySet()) {
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
