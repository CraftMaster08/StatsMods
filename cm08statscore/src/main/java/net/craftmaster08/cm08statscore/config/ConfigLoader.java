package net.craftmaster08.cm08statscore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.ChatFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogManager.getLogger(ConfigLoader.class);

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
                        LOGGER.warn("Invalid color '{}' for username '{}'", colorName, username);
                        continue;
                    }
                    tempUsernameColors.put(username, color);
                }
            }
            manager.usernameColors = tempUsernameColors;

            Set<String> tempBlacklistedPlayers = new HashSet<>();
            if (configJson.has("blacklisted_players")) {
                JsonArray blacklistedJson = configJson.getAsJsonArray("blacklisted_players");
                for (com.google.gson.JsonElement el : blacklistedJson) {
                    tempBlacklistedPlayers.add(el.getAsString());
                }
            }
            manager.blacklistedPlayers = tempBlacklistedPlayers;

            Map<String, Integer> tempIntLimits = new HashMap<>();
            if (configJson.has("intlimits")) {
                JsonObject intLimitsJson = configJson.getAsJsonObject("intlimits");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : intLimitsJson.entrySet()) {
                    try {
                        int limit = entry.getValue().getAsInt();
                        tempIntLimits.put(entry.getKey(), limit);
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid intlimit value for player '{}': {}", entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            manager.intLimits = tempIntLimits;

            if (configJson.has("daily_reset_time")) {
                manager.dailyResetTime = configJson.get("daily_reset_time").getAsString();
            }

            if (configJson.has("cooldown_seconds")) {
                manager.cooldownSeconds = configJson.get("cooldown_seconds").getAsInt();
            }

            LOGGER.info("Loaded statscore_config.json");
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Error loading statscore_config.json", e);
            resetToDefaults(manager);
        }
    }

    static void save(ConfigManager manager) {
        JsonObject configJson = new JsonObject();
        configJson.addProperty("daily_reset_time", manager.dailyResetTime);
        JsonObject usernameColorsJson = new JsonObject();
        manager.usernameColors.forEach((username, color) -> usernameColorsJson.addProperty(username, color.getName().toUpperCase()));
        configJson.add("username_colors", usernameColorsJson);
        JsonArray blacklistedJson = new JsonArray();
        manager.blacklistedPlayers.forEach(blacklistedJson::add);
        configJson.add("blacklisted_players", blacklistedJson);
        JsonObject intLimitsJson = new JsonObject();
        manager.intLimits.forEach(intLimitsJson::addProperty);
        configJson.add("intlimits", intLimitsJson);
        configJson.addProperty("cooldown_seconds", manager.cooldownSeconds);

        try (FileWriter writer = new FileWriter(ConfigManager.CONFIG_PATH.toFile())) {
            GSON.toJson(configJson, writer);
            LOGGER.info("Saved statscore_config.json");
        } catch (IOException e) {
            LOGGER.error("Failed to save statscore_config.json", e);
            throw new RuntimeException("Failed to save configuration", e);
        }
    }

    static void createDefaultConfig(File configFile) {
        JsonObject defaultConfig = new JsonObject();
        defaultConfig.addProperty("_comment", "DO NOT EDIT THIS FILE MANUALLY. Use /statsconfig commands to modify settings.");
        defaultConfig.addProperty("daily_reset_time", "00:00:00 UTC");
        defaultConfig.add("username_colors", new JsonObject());
        defaultConfig.add("blacklisted_players", new JsonArray());
        defaultConfig.add("intlimits", new JsonObject());
        defaultConfig.addProperty("cooldown_seconds", 5);

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(defaultConfig, writer);
            LOGGER.info("Created default statscore_config.json at {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create default statscore_config.json", e);
        }
    }

    private static void resetToDefaults(ConfigManager manager) {
        LOGGER.warn("Resetting to default configuration");
        manager.usernameColors = new HashMap<>();
        manager.blacklistedPlayers = new HashSet<>();
        manager.intLimits = new HashMap<>();
        manager.dailyResetTime = "00:00:00 UTC";
        manager.cooldownSeconds = 5;
    }
}