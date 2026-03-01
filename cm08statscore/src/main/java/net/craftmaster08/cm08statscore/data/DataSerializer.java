package net.craftmaster08.cm08statscore.data;

import com.google.gson.*;
import net.craftmaster08.cm08statscore.statstracker.ResetScheduler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DataSerializer {
    private static final Logger LOGGER = LogManager.getLogger(DataSerializer.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject loadJson(Path path) {
        File f = path.toFile();
        if (!f.exists()) return null;
        try (FileReader r = new FileReader(f)) {
            return GSON.fromJson(r, JsonObject.class);
        } catch (Exception e) {
            LOGGER.error("Failed to read {}", path.getFileName(), e);
            return null;
        }
    }

    public static void load(Path sharedPath, String dailyKey, String lastKnownKey,
                            Map<UUID, Double> dailyStats, ResetScheduler resetScheduler,
                            Map<UUID, Long> statLastKnownValue, String oldFileName,
                            double legacyConversionFactor) {
        dailyStats.clear();
        statLastKnownValue.clear();

        JsonObject dataJson = loadJson(sharedPath);
        if (dataJson == null) {
            dataJson = new JsonObject();
        }

        boolean migrated = false;
        boolean legacy = false;
        Instant migratedReset = null;

        if (!dataJson.has(dailyKey) && oldFileName != null && !oldFileName.isEmpty()) {
            Path oldPath = sharedPath.getParent().resolve(oldFileName);
            JsonObject oldJson = loadJson(oldPath);
            if (oldJson != null && oldJson.has(dailyKey)) {
                JsonObject dj = oldJson.getAsJsonObject(dailyKey);
                for (var e : dj.entrySet()) {
                    try {
                        dailyStats.put(UUID.fromString(e.getKey()), e.getValue().getAsDouble());
                    } catch (Exception ignored) {}
                }
                if (oldJson.has("last_reset_check")) {
                    try {
                        migratedReset = Instant.parse(oldJson.get("last_reset_check").getAsString());
                    } catch (Exception e) {
                        LOGGER.warn("Invalid last_reset_check in old file");
                    }
                }
                LOGGER.info("Migrated {} from old file {}", dailyKey, oldFileName);
                File oldFile = oldPath.toFile();
                if (oldFile.delete()) {
                    LOGGER.info("Deleted old file {}", oldFileName);
                } else {
                    LOGGER.warn("Failed to delete old file {}", oldFileName);
                }
                migrated = true;
                legacy = true;
            }
        }

        if (dataJson.has(dailyKey)) {
            JsonObject dj = dataJson.getAsJsonObject(dailyKey);
            for (var e : dj.entrySet()) {
                try {
                    dailyStats.put(UUID.fromString(e.getKey()), e.getValue().getAsDouble());
                } catch (Exception ignored) {}
            }
        }
        if (dataJson.has(lastKnownKey)) {
            JsonObject lk = dataJson.getAsJsonObject(lastKnownKey);
            for (var e : lk.entrySet()) {
                try {
                    statLastKnownValue.put(UUID.fromString(e.getKey()), e.getValue().getAsLong());
                } catch (Exception ignored) {}
            }
        }

        if (dataJson.has("last_reset_check")) {
            try {
                resetScheduler.setLastResetCheck(Instant.parse(dataJson.get("last_reset_check").getAsString()));
            } catch (Exception e) {
                LOGGER.warn("Invalid last_reset_check");
                resetScheduler.setLastResetCheck(Instant.now());
            }
        } else {
            resetScheduler.setLastResetCheck(Instant.now());
        }

        if (migrated) {
            if (migratedReset != null && !dataJson.has("last_reset_check")) {
                resetScheduler.setLastResetCheck(migratedReset);
            }
            // Add migrated data to dataJson
            JsonObject dailyJ = new JsonObject();
            dailyStats.forEach((u, v) -> dailyJ.addProperty(u.toString(), v));
            dataJson.add(dailyKey, dailyJ);

            JsonObject lkJ = new JsonObject();
            statLastKnownValue.forEach((u, v) -> lkJ.addProperty(u.toString(), v));
            dataJson.add(lastKnownKey, lkJ);

            dataJson.addProperty("last_reset_check", resetScheduler.getLastResetCheck().toString());

            // Save updated dataJson
            try (FileWriter w = new FileWriter(sharedPath.toFile())) {
                GSON.toJson(dataJson, w);
                LOGGER.info("Saved dailyStats.json after migration for {}", dailyKey);
            } catch (IOException e) {
                LOGGER.error("Failed to save dailyStats.json after migration", e);
            }
        }

        if (!migrated) {
            legacy = statLastKnownValue.isEmpty() && !dailyStats.isEmpty();
        }

        if (legacy && legacyConversionFactor != 1.0) {
            LOGGER.info("Legacy conversion x{} for {}", legacyConversionFactor, dailyKey);
            dailyStats.replaceAll((u, v) -> v * legacyConversionFactor);
            save(sharedPath, dailyKey, lastKnownKey, dailyStats, resetScheduler.getLastResetCheck(), statLastKnownValue);
        }
    }

    public static void save(Path sharedPath, String dailyKey, String lastKnownKey,
                            Map<UUID, Double> dailyStats, Instant lastResetCheck,
                            Map<UUID, Long> statLastKnownValue) {
        JsonObject dataJson = loadJson(sharedPath);
        if (dataJson == null) dataJson = new JsonObject();

        JsonObject dailyJ = new JsonObject();
        dailyStats.forEach((u, v) -> dailyJ.addProperty(u.toString(), v));
        dataJson.add(dailyKey, dailyJ);

        JsonObject lkJ = new JsonObject();
        statLastKnownValue.forEach((u, v) -> lkJ.addProperty(u.toString(), v));
        dataJson.add(lastKnownKey, lkJ);

        dataJson.addProperty("last_reset_check", lastResetCheck.toString());

        try (FileWriter w = new FileWriter(sharedPath.toFile())) {
            GSON.toJson(dataJson, w);
            if (!dailyStats.isEmpty()) {
                LOGGER.info("Saved {} to dailyStats.json", dailyKey);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save dailyStats.json", e);
        }
    }
}