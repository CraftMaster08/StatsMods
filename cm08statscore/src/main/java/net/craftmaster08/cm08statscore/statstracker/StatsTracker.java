package net.craftmaster08.cm08statscore.statstracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.craftmaster08.cm08statscore.resolver.UsernameResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StatsTracker {
    private static final Logger LOGGER = LogManager.getLogger(StatsTracker.class);
    public record StatsEntry(String username, double stat, UUID uuid) {}

    private final MinecraftServer server;
    private final String statisticType;
    private final String resourceLocation;
    private final String[] resourceLocations;

    public StatsTracker(MinecraftServer server, String statisticType, String resourceLocation) {
        this.server = server;
        this.statisticType = statisticType;
        this.resourceLocation = resourceLocation;
        this.resourceLocations = null;
    }

    public StatsTracker(MinecraftServer server, String statisticType, String[] resourceLocations) {
        this.server = server;
        this.statisticType = statisticType;
        this.resourceLocations = resourceLocations;
        this.resourceLocation = null;
    }

    public List<StatsEntry> getStats() {
        List<StatsEntry> entries = new ArrayList<>();

        File statsFolder = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        if (!fileExists(statsFolder)) {
            return entries;
        }

        File[] statFiles = statsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (statFiles == null) {
            return entries;
        }

        Set<UUID> onlineUUIDs = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toSet());

        for (File statFile : statFiles) {
            try {
                String uuidString = statFile.getName().replace(".json", "");
                UUID uuid = UUID.fromString(uuidString);

                if (onlineUUIDs.contains(uuid)) {
                    continue;
                }

                JsonObject statsJson;
                try (FileReader reader = new FileReader(statFile)) {
                    statsJson = JsonParser.parseReader(reader).getAsJsonObject();
                }

                JsonObject stats = statsJson.getAsJsonObject("stats");
                if (stats != null) {
                    JsonObject type = stats.getAsJsonObject(statisticType);
                    if (type != null) {
                        entries.add(getStatsEntry(type, uuid, uuidString));
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("Error reading stat file {}: {}", statFile.getName(), e.getMessage());
            }
        }
        return entries;
    }

    public StatsEntry getStatByUUID(UUID uuid) {
        File statsFolder = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        if (!statsFolder.exists() || !statsFolder.isDirectory()) {
            return null;
        }

        File statFile = new File(statsFolder, uuid.toString() + ".json");
        if (!statFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(statFile)) {
            JsonObject statsJson = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject stats = statsJson.getAsJsonObject("stats");
            if (stats == null) return null;

            JsonObject type = stats.getAsJsonObject(statisticType);
            if (type == null) return null;

            return getStatsEntry(type, uuid, uuid.toString());

        } catch (Exception e) {
            LOGGER.error("Error reading stat file for UUID {}: {}", uuid, e.getMessage());
            return null;
        }
    }

    private StatsEntry getStatsEntry(JsonObject type, UUID uuid, String uuidString) {
        JsonElement statElement;
        double stat = 0.0;
        String username = null;

        if (resourceLocation != null) {
            statElement = type.get("minecraft:%s".formatted(resourceLocation));
            if (statElement != null) {
                stat = statElement.getAsLong();
                username = UsernameResolver.resolve(server, uuid, uuidString);
            }
        }
        if (resourceLocations != null) {
            for (String location : resourceLocations) {
                statElement = type.get("minecraft:%s".formatted(location));
                if (statElement != null) {
                    stat += statElement.getAsLong();
                    username = UsernameResolver.resolve(server, uuid, uuidString);

                }
            }
        }

        return new StatsEntry(username, stat, uuid);
    }

    public static boolean fileExists(File statsFolder) {
        return statsFolder.exists() && statsFolder.isDirectory();
    }
}
