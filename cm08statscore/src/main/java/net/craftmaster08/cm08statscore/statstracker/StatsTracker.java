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
    private static final int UInt32Limit = 2147483647;
    private static final Logger LOGGER = LogManager.getLogger(StatsTracker.class);
    public record StatsEntry(String username, double stat, UUID uuid) {}

    private static final String[] OFFLINE_DISTANCE_STATS = {
            "minecraft:walk_one_cm", "minecraft:sprint_one_cm", "minecraft:crouch_one_cm", "minecraft:swim_one_cm",
            "minecraft:fall_one_cm", "minecraft:climb_one_cm", "minecraft:fly_one_cm", "minecraft:walk_on_water_one_cm",
            "minecraft:walk_under_water_one_cm", "minecraft:minecart_one_cm", "minecraft:boat_one_cm", "minecraft:pig_one_cm",
            "minecraft:horse_one_cm", "minecraft:aviate_one_cm"
    };

    static String getStatIdFromType(LeaderboardFormatter.StatsType type) {
        return switch (type) {
            case PLAYTIME -> "play_time";
            case DISTANCE -> null;
            case DEATHS -> "deaths";
            case KILLS -> "player_kills";
        };
    }

    public static double calculatePlayerDistance(ServerPlayer player) {
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

            int intLimitCount = StatsCore.getConfigManager().getIntLimits().getOrDefault(player.getName().getString(), 0);
            totalDistanceCm += (double) intLimitCount * UInt32Limit;
        } catch (Exception e) {
            LOGGER.error("Error calculating distance for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return totalDistanceCm;
    }

    public static List<StatsEntry> getOverallStats(MinecraftServer server, LeaderboardFormatter.StatsType type) {
        List<StatsEntry> online = getOnlineStats(server, type);
        List<StatsEntry> offline = getOfflineStats(server, type);

        return Stream.concat(online.stream(), offline.stream())
                .sorted(Comparator.comparingDouble(StatsEntry::stat).reversed())
                .toList();
    }

    public static List<StatsEntry> getOnlineStats(MinecraftServer server, LeaderboardFormatter.StatsType type) {
        List<StatsEntry> entries = new ArrayList<>();

        switch (type) {
            case PLAYTIME -> entries = server.getPlayerList().getPlayers().stream()
                    .map(player -> new StatsEntry(
                            player.getName().getString(),
                            player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 20.0 / 3600.0,
                            player.getUUID()
                    ))
                    .toList();

            case DISTANCE -> entries = server.getPlayerList().getPlayers().stream()
                    .map(player -> {
                        double totalDistanceCm = calculatePlayerDistance(player);
                        double distanceKm = totalDistanceCm / 100000.0; // Convert cm to km
                        return new StatsEntry(
                                player.getName().getString(),
                                distanceKm,
                                player.getUUID()
                        );
                    })
                    .toList();

            case DEATHS -> entries = server.getPlayerList().getPlayers().stream()
                    .map(player -> new StatsEntry(
                            player.getName().getString(),
                            player.getStats().getValue(Stats.CUSTOM.get(DEATHS)),
                            player.getUUID()
                    ))
                    .toList();

            case KILLS -> entries = server.getPlayerList().getPlayers().stream()
                    .map(player -> new StatsEntry(
                            player.getName().getString(),
                            player.getStats().getValue(Stats.ENTITY_KILLED.get(EntityType.PLAYER)),
                            player.getUUID()
                    ))
                    .toList();
        }
        return entries;
    }

    public static List<StatsEntry> getOfflineStats(MinecraftServer server, LeaderboardFormatter.StatsType type) {
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
                    JsonObject custom = stats.getAsJsonObject("minecraft:custom");
                    if (custom != null) {
                        switch (type) {
                            case PLAYTIME -> {
                                JsonElement playTimeElement = custom.get("minecraft:%s".formatted(getStatIdFromType(LeaderboardFormatter.StatsType.PLAYTIME)));
                                if (playTimeElement != null) {
                                    double hours = playTimeElement.getAsLong() / 20.0 / 3600.0;
                                    String username = UsernameResolver.resolve(server, uuid, uuidString);
                                    entries.add(new StatsEntry(username, hours, uuid));
                                }
                            }

                            case DISTANCE -> {
                                double totalDistanceCm = 0.0;
                                for (String stat : OFFLINE_DISTANCE_STATS) {
                                    JsonElement element = custom.get(stat);
                                    if (element != null) {
                                        totalDistanceCm += element.getAsLong();
                                    }
                                }
                                String username = UsernameResolver.resolve(server, uuid, uuidString);
                                int intLimitCount = StatsCore.getConfigManager().getIntLimits().getOrDefault(username, 0);
                                totalDistanceCm += (double) intLimitCount * UInt32Limit;
                                double distanceKm = totalDistanceCm / 100000.0; // Convert cm to km
                                entries.add(new StatsEntry(username, distanceKm, uuid));
                            }

                            case DEATHS -> {
                                JsonElement deathElement = custom.get(String.format("minecraft:%s", getStatIdFromType(LeaderboardFormatter.StatsType.DEATHS)));
                                if (deathElement != null) {
                                    int deathsCount = deathElement.getAsInt();
                                    String username = UsernameResolver.resolve(server, uuid, uuidString);
                                    entries.add(new StatsEntry(username, deathsCount, uuid));
                                } else {
                                    entries.add(new StatsEntry(UsernameResolver.resolve(server, uuid, uuidString), 0, uuid));
                                }
                            }

                            case KILLS -> {
                                JsonElement killElement = custom.get(String.format("minecraft:%s", getStatIdFromType(LeaderboardFormatter.StatsType.KILLS)));
                                if (killElement != null) {
                                    int killsCount = killElement.getAsInt();
                                    String username = UsernameResolver.resolve(server, uuid, uuidString);
                                    entries.add(new StatsEntry(username, killsCount, uuid));
                                } else {
                                    entries.add(new StatsEntry(UsernameResolver.resolve(server, uuid, uuidString), 0, uuid));
                                }
                            }
                        }
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("Error reading stat file {}: {}", statFile.getName(), e.getMessage());
            }
        }
        return entries;
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
