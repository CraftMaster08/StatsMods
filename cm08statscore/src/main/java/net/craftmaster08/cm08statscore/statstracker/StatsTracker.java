package net.craftmaster08.cm08statscore.statstracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.cache.UsernameCache;
import net.craftmaster08.cm08statscore.ranking.LeaderboardFormatter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraft.stats.Stats.DEATHS;
import static net.minecraft.stats.Stats.ENTITY_KILLED;

public class StatsTracker {
    private static final Logger LOGGER = LogManager.getLogger(StatsTracker.class);
    public record StatsEntry(String username, double stat, UUID uuid) {}

    public static List<StatsEntry> getStats(MinecraftServer server, String statisticType, String resourceLocation) {
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
                        JsonElement statElement = type.get("minecraft:%s".formatted(resourceLocation));
                        if (statElement != null) {
                            double stat = statElement.getAsLong();
                            String username = UsernameResolver.resolve(server, uuid, uuidString);
                            entries.add(new StatsEntry(username, stat, uuid));
                        }
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("Error reading stat file {}: {}", statFile.getName(), e.getMessage());
            }
        }
        return entries;
    }

    public static StatsEntry getStatByUUID(MinecraftServer server, String statisticType, String resourceLocation, UUID uuid) {
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

            JsonElement statElement = type.get("minecraft:" + resourceLocation);
            if (statElement == null) return null;

            long value = statElement.getAsLong();
            String username = UsernameResolver.resolve(server, uuid, uuid.toString());
            return new StatsEntry(username, value, uuid);

        } catch (Exception e) {
            LOGGER.error("Error reading stat file for UUID {}: {}", uuid, e.getMessage());
            return null;
        }
    }

    public static String formatDistance(double distanceKm) {
        if (distanceKm >= 1000.0) {
            return String.format("%dkm", (int) distanceKm);
        } else if (distanceKm >= 1.0) {
            return String.format("%.2fkm", distanceKm);
        } else {
            double meters = distanceKm * 1000.0;
            return String.format("%dm", (int) meters);
        }
    }

    public static boolean fileExists(File statsFolder) {
        return statsFolder.exists() && statsFolder.isDirectory();
    }

    private interface UsernameResolver {
        static String resolve(MinecraftServer server, UUID uuid, String uuidString) {
            // Try server profile cache
            String username = server.getProfileCache()
                    .get(uuid)
                    .map(profile -> {
                        String name = profile.getName();
                        if (name == null || name.isEmpty()) {
                            LOGGER.warn("Profile cache returned null/empty name for UUID: {}", uuidString);
                            return null;
                        }
                        return name;
                    })
                    .orElse(null);

            if (username != null) {
                return username;
            }

            // Try custom cache
            UsernameCache cache = UsernameCache.getInstance(server);
            username = cache.getUsername(uuid);
            if (username != null) {
                return username;
            }

            // Fallback to Mojang API
            try {
                HttpClient client = HttpClient.newHttpClient();
                String uuidNoHyphens = uuidString.replace("-", "");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mojang.com/user/profile/" + uuidNoHyphens))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    username = json.get("name").getAsString();
                    if (username != null && !username.isEmpty()) {
                        cache.storeUsername(uuid, username);
                        return username;
                    }
                } else {
                    LOGGER.warn("Mojang API request failed for UUID: {}, status: {}", uuidString, response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error querying Mojang API for UUID: {}", uuidString, e);
            }

            // Final fallback
            return "Unknown_" + uuidString.substring(0, 8);
        }
    }
}
