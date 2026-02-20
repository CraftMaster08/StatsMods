package net.craftmaster08.cm08statscore.statstracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.craftmaster08.cm08statscore.resolver.UsernameResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

public class StatsTracker {
    private static final Logger LOGGER = LogManager.getLogger(StatsTracker.class);
    public record StatsEntry(String username, double stat, UUID uuid) {}

    private final MinecraftServer server;
    private final String statisticType;
    private final String resourceLocation;
    private final String[] resourceLocations;
    private final Stat<ResourceLocation> liveStatOverride;
    private final List<Stat<ResourceLocation>> liveStatOverrides;

    public StatsTracker(MinecraftServer server, String statisticType, String resourceLocation, Stat<ResourceLocation> liveStatOverride) {
        this.server = server;
        this.statisticType = statisticType;
        this.resourceLocation = resourceLocation;
        this.resourceLocations = null;
        this.liveStatOverride = liveStatOverride;
        this.liveStatOverrides = null;
    }

    public StatsTracker(MinecraftServer server, String statisticType, String[] resourceLocations, List<Stat<ResourceLocation>> liveStatOverrides) {
        this.server = server;
        this.statisticType = statisticType;
        this.resourceLocation = null;
        this.resourceLocations = resourceLocations;
        this.liveStatOverride = null;
        this.liveStatOverrides = liveStatOverrides;
    }

    public List<StatsEntry> getStats() {
        List<StatsEntry> entries = new ArrayList<>();

        // online
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            String username = player.getGameProfile().getName();
            long statValue = getLiveStatValue(player);
            entries.add(new StatsEntry(username, statValue, uuid));
        }

        // offline
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

                if (onlineUUIDs.contains(uuid)) continue;

                try (FileReader reader = new FileReader(statFile)) {
                    JsonObject statsJson = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject stats = statsJson.getAsJsonObject("stats");
                    if (stats == null) continue;

                    JsonObject type = stats.getAsJsonObject(statisticType);
                    if (type == null) continue;

                    double stat = calculateStatFromType(type);
                    String username = UsernameResolver.resolve(server, uuid, uuidString);
                    entries.add(new StatsEntry(username, stat, uuid));
                }
            } catch (Exception e) {
                LOGGER.error("Error reading stat file {}: {}", statFile.getName(), e.getMessage());
            }
        }

        return entries;
    }

    public StatsEntry getStatByUUID(UUID uuid) {
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            long value = getLiveStatValue(onlinePlayer);
            return new StatsEntry(onlinePlayer.getGameProfile().getName(), value, uuid);
        }

        File statsFolder = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        File statFile = new File(statsFolder, uuid + ".json");
        if (!statFile.exists()) return null;

        try (FileReader reader = new FileReader(statFile)) {
            JsonObject statsJson = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject stats = statsJson.getAsJsonObject("stats");
            if (stats == null) return null;

            JsonObject type = stats.getAsJsonObject(statisticType);
            if (type == null) return null;

            double stat = calculateStatFromType(type);
            String username = UsernameResolver.resolve(server, uuid, uuid.toString());
            return new StatsEntry(username, stat, uuid);
        } catch (Exception e) {
            LOGGER.error("Error reading stat file for UUID {}: {}", uuid, e.getMessage());
            return null;
        }
    }

    private long getLiveStatValue(ServerPlayer player) {
        try {
            if (resourceLocation != null) {
                if (liveStatOverride != null) {
                    return player.getStats().getValue(liveStatOverride);
                }
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("minecraft", resourceLocation);
                LOGGER.info("[LIVE] Attempting stat lookup for key: {}", rl);
                Stat<ResourceLocation> stat = Stats.CUSTOM.get(rl);
                LOGGER.info("[LIVE] Stat<ResLoc> is {}", stat);
                return player.getStats().getValue(stat);
            }

            if (resourceLocations != null) {
                if (liveStatOverrides != null) {
                    long total = 0;
                    for (Stat<ResourceLocation> loc : liveStatOverrides) {
                        total += player.getStats().getValue(loc);
                    }
                    return total;
                }
                long total = 0;
                for (String loc : resourceLocations) {
                    ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("minecraft", loc);
                    LOGGER.info("ResourceLocation: {}", rl);
                    Stat<ResourceLocation> stat = Stats.CUSTOM.get(rl);
                    total += player.getStats().getValue(stat);
                }
                return total;
            }
            return 0;
        } catch (Exception e) {
            LOGGER.error("Live stat fail for {}: {}", player.getScoreboardName(), e.getMessage());
            return 0;
        }
    }

    private double calculateStatFromType(JsonObject type) {
        double stat = 0.0;
        if (resourceLocation != null) {
            JsonElement el = type.get("minecraft:" + resourceLocation);
            if (el != null) stat = el.getAsLong();
        }
        if (resourceLocations != null) {
            for (String loc : resourceLocations) {
                JsonElement el = type.get("minecraft:" + loc);
                if (el != null) stat += el.getAsLong();
            }
        }
        return stat;
    }

    public static boolean fileExists(File statsFolder) {
        return statsFolder.exists() && statsFolder.isDirectory();
    }
}
