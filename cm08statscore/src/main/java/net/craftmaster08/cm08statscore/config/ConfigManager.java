package net.craftmaster08.cm08statscore.config;

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

    public Map<String, ChatFormatting> usernameColors;
    public Set<String> blacklistedPlayers;
    public Map<String, Integer> intLimits;
    public String dailyResetTime;

    public ConfigManager() {
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
        if (!configFile.exists()) {
            LOGGER.info("No config file found; creating default statscore_config.json");
            ConfigLoader.createDefaultConfig(configFile);
        }

        ConfigLoader.load(configFile, this);
    }
}