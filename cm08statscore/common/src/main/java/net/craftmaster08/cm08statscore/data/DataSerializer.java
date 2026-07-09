package net.craftmaster08.cm08statscore.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.craftmaster08.cm08statscore.statstracker.ResetScheduler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DataSerializer {
    private static final Logger LOGGER = LogManager.getLogger(DataSerializer.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject loadJson(Path path) {
        if (!path.toFile().exists()) return null;
        try (FileReader r = new FileReader(path.toFile())) {
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

        if (oldFileName != null) {
            Path oldPath = sharedPath.getParent().resolve(oldFileName);
            JsonObject oldData = loadJson(oldPath);
            if (oldData != null) {
                JsonObject dailyJ = oldData.getAsJsonObject(dailyKey);
                if (dailyJ != null) {
                    dailyJ.entrySet().forEach(e -> {
                        try {
                            UUID u = UUID.fromString(e.getKey());
                            double v = e.getValue().getAsDouble();
                            dailyStats.put(u, v);
                        } catch (Exception ex) {
                            LOGGER.warn("Invalid entry in old file: {}", e.getKey());
                        }
                    });
                    migrated = true;
                    migratedReset = Instant.now();
                    oldPath.toFile().delete();
                    LOGGER.info("Migrated old {} to dailyStats.json", oldFileName);
                }
            }
        }

        JsonObject dailyJ = dataJson.getAsJsonObject(dailyKey);
        if (dailyJ != null) {
            dailyJ.entrySet().forEach(e -> {
                try {
                    UUID u = UUID.fromString(e.getKey());
                    double v = e.getValue().getAsDouble();
                    dailyStats.put(u, v);
                } catch (Exception ex) {
                    LOGGER.warn("Invalid daily entry: {}", e.getKey());
                }
            });
        }

        JsonObject lkJ = dataJson.getAsJsonObject(lastKnownKey);
        if (lkJ != null) {
            lkJ.entrySet().forEach(e -> {
                try {
                    UUID u = UUID.fromString(e.getKey());
                    long v = e.getValue().getAsLong();
                    statLastKnownValue.put(u, v);
                } catch (Exception ex) {
                    LOGGER.warn("Invalid last known entry: {}", e.getKey());
                }
            });
        }

        if (dataJson.has("last_reset_check")) {
            String lastResetStr = dataJson.get("last_reset_check").getAsString();
            resetScheduler.setLastResetCheck(Instant.parse(lastResetStr));
        } else if (migrated) {
            resetScheduler.setLastResetCheck(migratedReset);
        } else {
            resetScheduler.setLastResetCheck(Instant.now());
        }

        if (migrated) {
            save(sharedPath, dailyKey, lastKnownKey, dailyStats, resetScheduler.getLastResetCheck(), statLastKnownValue);
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
