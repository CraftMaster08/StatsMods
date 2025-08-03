/*package net.craftmaster08.cm08statscore.distance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyDistanceTracker {
    private static final Logger LOGGER = LogManager.getLogger(DailyDistanceTracker.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dataPath;
    private final MinecraftServer server;
    private final ResetScheduler resetScheduler;
    private final Map<UUID, Double> dailyDistances;
    private final Map<UUID, Long> lastKnownTicks;

    public DailyDistanceTracker(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("MinecraftServer cannot be null");
        }
        this.server = server;
        this.dataPath = server.getWorldPath(LevelResource.ROOT).resolve("distance_daily.json");
        this.dailyDistances = new HashMap<>();
        this.lastKnownTicks = new HashMap<>();
        this.resetScheduler = new ResetScheduler(this);
        setDailyResetTime("00:00:00 UTC"); // Default reset time, can be set externally
        loadData();
    }

    public void setDailyResetTime(String timeStr) {
        resetScheduler.setDailyResetTime(timeStr);
    }

    public double getDailyDistance(UUID uuid) {
        return dailyDistances.getOrDefault(uuid, 0.0);
    }

    public void updatePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        double currentDistanceCm = calculatePlayerDistance(player);
        double lastDistanceCm = lastKnownTicks.getOrDefault(uuid, (long) currentDistanceCm).doubleValue();

        double distanceTraveledCm = currentDistanceCm - lastDistanceCm;
        dailyDistances.merge(uuid, distanceTraveledCm, Double::sum);
        lastKnownTicks.put(uuid, (long) currentDistanceCm);

        resetScheduler.checkReset();
    }

    public void playerLoggedIn(ServerPlayer player) {
        UUID uuid = player.getUUID();
        double currentDistanceCm = calculatePlayerDistance(player);
        lastKnownTicks.put(uuid, (long) currentDistanceCm);
        dailyDistances.putIfAbsent(uuid, 0.0);
    }

    public void playerLoggedOut(ServerPlayer player) {
        updatePlayer(player);
        saveData();
    }

    private double calculatePlayerDistance(ServerPlayer player) {
        double totalDistanceCm = 0.0;
        try {
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.CROUCH_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.FALL_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.CLIMB_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ON_WATER_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_UNDER_WATER_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.MINECART_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.BOAT_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.PIG_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.HORSE_ONE_CM));
            totalDistanceCm += player.getStats().getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM));
        } catch (Exception e) {
            LOGGER.error("Error calculating distance for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return totalDistanceCm;
    }

    private void loadData() {
        DataSerializer.load(dataPath, dailyDistances, resetScheduler);
    }

    private void saveData() {
        DataSerializer.save(dataPath, dailyDistances, resetScheduler.getLastResetCheck());
    }

    public static String formatDailyDistance(double distanceCm) {
        double totalMeters = distanceCm / 100.0; // Convert cm to meters
        int km = (int) (totalMeters / 1000); // Whole kilometers
        int m = (int) (totalMeters % 1000); // Remaining meters
        return String.format("%dkm %dm today", km, m);
    }

    private static class ResetScheduler {
        private String dailyResetTime;
        private LocalTime resetTime;
        private Instant lastResetCheck;
        private final DailyDistanceTracker tracker;

        ResetScheduler(DailyDistanceTracker tracker) {
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
                this.dailyResetTime = timeStr;
                LOGGER.info("Set daily reset time to: {}", timeStr);
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
                tracker.saveData();
            }

            if (!currentZdt.toLocalDate().equals(lastCheckZdt.toLocalDate())) {
                ZonedDateTime tomorrowReset = todayReset.plusDays(1);
                if (currentZdt.isAfter(tomorrowReset) && lastCheckZdt.isBefore(tomorrowReset)) {
                    LOGGER.info("Resetting daily distance at {}", currentZdt);
                    tracker.dailyDistances.replaceAll((uuid, v) -> 0.0);
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
        static void load(Path dataPath, Map<UUID, Double> dailyDistances, ResetScheduler resetScheduler) {
            File dataFile = dataPath.toFile();
            if (!dataFile.exists()) {
                save(dataPath, dailyDistances, resetScheduler.getLastResetCheck());
                return;
            }

            try (FileReader reader = new FileReader(dataFile)) {
                JsonObject dataJson = GSON.fromJson(reader, JsonObject.class);
                if (dataJson == null) {
                    throw new JsonParseException("Daily distance file is empty or invalid JSON");
                }

                if (dataJson.has("daily_distances")) {
                    JsonObject distancesJson = dataJson.getAsJsonObject("daily_distances");
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : distancesJson.entrySet()) {
                        try {
                            UUID uuid = UUID.fromString(entry.getKey());
                            dailyDistances.put(uuid, entry.getValue().getAsDouble());
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid UUID in distance data: {}", entry.getKey());
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

                LOGGER.info("Successfully loaded distance_daily.json");
            } catch (IOException | JsonParseException e) {
                LOGGER.error("Failed to load distance_daily.json", e);
                dailyDistances.clear();
                resetScheduler.lastResetCheck = Instant.now();
            }
        }

        static void save(Path dataPath, Map<UUID, Double> dailyDistances, Instant lastResetCheck) {
            JsonObject dataJson = new JsonObject();
            JsonObject distancesJson = new JsonObject();
            dailyDistances.forEach((uuid, distance) -> distancesJson.addProperty(uuid.toString(), distance));
            dataJson.add("daily_distances", distancesJson);
            dataJson.addProperty("last_reset_check", lastResetCheck.toString());

            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(dataJson, writer);
                LOGGER.info("Saved distance_daily.json");
            } catch (IOException e) {
                LOGGER.error("Failed to save distance_daily.json", e);
            }
        }
    }
}

 */