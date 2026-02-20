package net.craftmaster08.cm08statscore.data;

import com.google.gson.*;
import net.craftmaster08.cm08statscore.statstracker.ResetScheduler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

public class DataSerializer {
    private static final Logger LOGGER = LogManager.getLogger(DataSerializer.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dataPath, Map<UUID, Double> dailyStats, ResetScheduler resetScheduler, String statsFile, Map<UUID, Long> statLastKnownValue) {
        File dataFile = dataPath.toFile();
        if (!dataFile.exists()) {
            save(dataPath, dailyStats, resetScheduler.getLastResetCheck(), statsFile, statLastKnownValue);
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
                    resetScheduler.setLastResetCheck(Instant.parse(dataJson.get("last_reset_check").getAsString()));
                } catch (DateTimeParseException e) {
                    LOGGER.warn("Invalid last_reset_check format, using current time");
                    resetScheduler.setLastResetCheck(Instant.now());
                }
            }

            if (dataJson.has("last_known_values")) {
                JsonObject lastKnownJson = dataJson.getAsJsonObject("last_known_values");
                lastKnownJson.entrySet().forEach(entry -> {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        statLastKnownValue.put(uuid, entry.getValue().getAsLong());
                    } catch (Exception ignored) {}
                });
            }

            LOGGER.info(String.format("Successfully loaded %s.json", dataFile));
        } catch (IOException | JsonParseException e) {
            LOGGER.error(String.format("Failed to load %s.json", dataFile), e);
            dailyStats.clear();
            resetScheduler.setLastResetCheck(Instant.now());
        }
    }

    public static void save(Path dataPath, Map<UUID, Double> dailyStats, Instant lastResetCheck, String dataFile, Map<UUID, Long> statLastKnownValue) {
        JsonObject dataJson = new JsonObject();
        JsonObject statsJson = new JsonObject();
        dailyStats.forEach((uuid, hours) -> statsJson.addProperty(uuid.toString(), hours));
        dataJson.add(dataFile, statsJson);

        JsonObject lastKnownJson = new JsonObject();
        statLastKnownValue.forEach((uuid, value) -> lastKnownJson.addProperty(uuid.toString(), value));
        dataJson.add("last_known_values", lastKnownJson);

        dataJson.addProperty("last_reset_check", lastResetCheck.toString());
        try (FileWriter writer = new FileWriter(dataPath.toFile())) {
            GSON.toJson(dataJson, writer);
            LOGGER.info(String.format("Saved %s.json", dataFile));
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to save %s.json", dataFile), e);
        }
    }
}