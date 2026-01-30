package net.craftmaster08.cm08statscore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.ChatFormatting;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);
    public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("statscore_config.json");
    //public static final Path OLD_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("playtimeleaderboard_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Map<String, ChatFormatting> usernameColors;
    public Set<String> blacklistedPlayers;
    public Map<String, Integer> intLimits;
    public String dailyResetTime;
    //public final DailyStatsTracker dailyStatsTracker;

    public ConfigManager() {
        //this.dailyStatsTracker = dailyPlaytimeTracker;
        //if (dailyStatsTracker == null) {
        //    LOGGER.warn("DailyStatsTracker is null; daily stats features will be disabled");
        //}
        this.usernameColors = Map.of();
        this.blacklistedPlayers = Set.of();
        this.intLimits = Map.of();
        this.dailyResetTime = "00:00:00";
        loadConfig();
    }

    public Map<String, ChatFormatting> getUsernameColors() {
        return usernameColors;
    }

    public Set<String> getBlacklistedPlayers() {
        return blacklistedPlayers;
    }

    public Map<String, Integer> getIntLimits() { return intLimits; }

    public void loadConfig() {
        File configFile = CONFIG_PATH.toFile();
        //File oldConfigFile = OLD_CONFIG_PATH.toFile();

        /*
        // Warn if old config exists alongside new config
        if (oldConfigFile.exists() && configFile.exists()) {
            LOGGER.warn("Old configuration file '{}' exists alongside '{}'. The old file is ignored and should be deleted manually to avoid confusion. It does not affect mod functionality.",
                    OLD_CONFIG_PATH.getFileName(), CONFIG_PATH.getFileName());
        }
*/
        // Migrate old config if it exists and new config does not
        /*
        if (oldConfigFile.exists() && !configFile.exists()) {
            LOGGER.info("Found old playtimeleaderboard_config.json; migrating to statscore_config.json");
            if (migrateOldConfig(oldConfigFile, configFile)) {
                LOGGER.info("Successfully migrated old config to {}", configFile.getAbsolutePath());
            } else {
                LOGGER.error("Failed to migrate old config; creating default statscore_config.json");
                ConfigLoader.createDefaultConfig(configFile);
            }
        }
        */

        if (!configFile.exists()) {
            LOGGER.info("No config file found; creating default statscore_config.json");
            ConfigLoader.createDefaultConfig(configFile);
        }

        //ConfigLoader.load(configFile, this);
        //if (dailyStatsTracker != null) {
        //    dailyStatsTracker.setDailyResetTime(dailyResetTime);
        //} else {
        //    LOGGER.warn("Skipping daily reset time due to null tracker");
        //}
    }

    /*
    private boolean migrateOldConfig(File oldConfigFile, File newConfigFile) {
        try (FileReader reader = new FileReader(oldConfigFile)) {
            JsonObject oldConfigJson = GSON.fromJson(reader, JsonObject.class);
            if (oldConfigJson == null) {
                throw new JsonParseException("Old config file is empty or invalid JSON");
            }

            // Extract data from old config
            Map<String, ChatFormatting> tempUsernameColors = new HashMap<>();
            if (oldConfigJson.has("username_colors")) {
                JsonObject usernameColorsJson = oldConfigJson.getAsJsonObject("username_colors");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : usernameColorsJson.entrySet()) {
                    String username = entry.getKey();
                    String colorName = entry.getValue().getAsString().toUpperCase();
                    ChatFormatting color = ChatFormatting.getByName(colorName);
                    if (color == null || !color.isColor()) {
                        LOGGER.warn("Invalid Minecraft color in old config for {}: {}", username, colorName);
                        continue;
                    }
                    tempUsernameColors.put(username, color);
                }
            }

            Set<String> tempBlacklistedPlayers = new HashSet<>();
            if (oldConfigJson.has("blacklisted_players")) {
                oldConfigJson.getAsJsonArray("blacklisted_players")
                        .forEach(element -> {
                            String player = element.getAsString();
                            tempBlacklistedPlayers.add(player);
                            LOGGER.info("Migrated blacklisted player: {}", player);
                        });
            }

            Map<String, Integer> tempIntLimits = new HashMap<>();
            if (oldConfigJson.has("intlimits")) {
                JsonObject intLimitsJson = oldConfigJson.getAsJsonObject("intlimits");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : intLimitsJson.entrySet()) {
                    String username = entry.getKey();
                    int limitCount = entry.getValue().getAsInt();
                    if (limitCount >= 0) {
                        tempIntLimits.put(username, limitCount);
                        LOGGER.info("Migrated intlimit for {}: {}", username, limitCount);
                    } else {
                        LOGGER.warn("Invalid intlimit count for {}: {}", username, limitCount);
                    }
                }
            }

            String tempDailyResetTime = oldConfigJson.has("daily_reset_time")
                    ? oldConfigJson.get("daily_reset_time").getAsString().replaceAll("\\s*UTC.*", "")
                    : "00:00:00";

            // Create new config in the new format
            JsonObject newConfig = new JsonObject();
            newConfig.addProperty("_comment", "DO NOT EDIT THIS FILE MANUALLY. Use /statsconfig commands to modify settings.");
            newConfig.addProperty("daily_reset_time", tempDailyResetTime);

            JsonObject usernameColorsJson = new JsonObject();
            tempUsernameColors.forEach((username, color) -> usernameColorsJson.addProperty(username, color.getName().toUpperCase()));
            newConfig.add("username_colors", usernameColorsJson);

            com.google.gson.JsonArray blacklistedPlayersArray = new com.google.gson.JsonArray();
            tempBlacklistedPlayers.forEach(blacklistedPlayersArray::add);
            newConfig.add("blacklisted_players", blacklistedPlayersArray);

            JsonObject intLimitsJson = new JsonObject();
            tempIntLimits.forEach(intLimitsJson::addProperty);
            newConfig.add("intlimits", intLimitsJson);

            // Write new config to statscore_config.json
            try (FileWriter writer = new FileWriter(newConfigFile)) {
                GSON.toJson(newConfig, writer);
                LOGGER.info("Wrote migrated config to {}", newConfigFile.getAbsolutePath());
            }

            // Update ConfigManager fields
            this.usernameColors = Map.copyOf(tempUsernameColors);
            this.blacklistedPlayers = Set.copyOf(tempBlacklistedPlayers);
            this.intLimits = Map.copyOf(tempIntLimits);
            this.dailyResetTime = tempDailyResetTime;
            return true;
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to migrate old playtimeleaderboard_config.json", e);
            return false;
        }
    }
*/

    private static class ConfigLoader {
        static void load(File configFile, ConfigManager manager) {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject configJson = GSON.fromJson(reader, JsonObject.class);
                if (configJson == null) {
                    throw new JsonParseException("Config file is empty or invalid JSON");
                }

                Map<String, ChatFormatting> tempUsernameColors = new HashMap<>();
                if (configJson.has("username_colors")) {
                    JsonObject usernameColorsJson = configJson.getAsJsonObject("username_colors");
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : usernameColorsJson.entrySet()) {
                        String username = entry.getKey();
                        String colorName = entry.getValue().getAsString().toUpperCase();
                        ChatFormatting color = ChatFormatting.getByName(colorName);
                        if (color == null || !color.isColor()) {
                            LOGGER.warn("Invalid Minecraft color for {}: {}", username, colorName);
                            continue;
                        }
                        tempUsernameColors.put(username, color);
                    }
                }

                Set<String> tempBlacklistedPlayers = new HashSet<>();
                if (configJson.has("blacklisted_players")) {
                    configJson.getAsJsonArray("blacklisted_players")
                            .forEach(element -> {
                                String player = element.getAsString();
                                tempBlacklistedPlayers.add(player);
                                LOGGER.info("Loaded blacklisted player: {}", player);
                            });
                }

                Map<String, Integer> tempIntLimits = new HashMap<>();
                if (configJson.has("intlimits")) {
                    JsonObject intLimitsJson = configJson.getAsJsonObject("intlimits");
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : intLimitsJson.entrySet()) {
                        String username = entry.getKey();
                        int limitCount = entry.getValue().getAsInt();
                        if (limitCount >= 0) {
                            tempIntLimits.put(username, limitCount);
                            LOGGER.info("Loaded intlimit for {}: {}", username, limitCount);
                        } else {
                            LOGGER.warn("Invalid intlimit count for {}: {}", username, limitCount);
                        }
                    }
                }

                String tempDailyResetTime = configJson.has("daily_reset_time")
                        ? configJson.get("daily_reset_time").getAsString()
                        : "00:00:00";

                manager.usernameColors = Map.copyOf(tempUsernameColors);
                manager.blacklistedPlayers = Set.copyOf(tempBlacklistedPlayers);
                manager.intLimits = Map.copyOf(tempIntLimits);
                manager.dailyResetTime = tempDailyResetTime;
                LOGGER.info("Successfully loaded statscore_config.json");
                LOGGER.info("Blacklisted players after loading: {}", tempBlacklistedPlayers);
                LOGGER.info("Intlimits after loading: {}", tempIntLimits);
            } catch (IOException | JsonParseException e) {
                LOGGER.error("Failed to load statscore_config.json", e);
                resetToDefaults(manager);
            }
        }

        static void createDefaultConfig(File configFile) {
            JsonObject defaultConfig = new JsonObject();
            defaultConfig.addProperty("_comment", "DO NOT EDIT THIS FILE MANUALLY. Use /statsconfig commands to modify settings.");
            defaultConfig.addProperty("daily_reset_time", "00:00:00");
            defaultConfig.add("username_colors", new JsonObject());
            defaultConfig.add("blacklisted_players", new com.google.gson.JsonArray());
            defaultConfig.add("intlimits", new JsonObject());

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(defaultConfig, writer);
                LOGGER.info("Created default statscore_config.json at {}", configFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to create default statscore_config.json", e);
            }
        }

        private static void resetToDefaults(ConfigManager manager) {
            LOGGER.warn("Resetting to default configuration");
            manager.usernameColors = Map.of();
            manager.blacklistedPlayers = Set.of();
            manager.intLimits = Map.of();
            manager.dailyResetTime = "00:00:00";
            //if (manager.dailyStatsTracker != null) {
            //    manager.dailyStatsTracker.setDailyResetTime(manager.dailyResetTime);
            //} else {
            //    LOGGER.warn("Skipping dailyPlaytimeTracker.setDailyResetTime in resetToDefaults due to null tracker");
            //}
        }
    }
}