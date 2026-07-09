package net.craftmaster08.cm08statscore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.craftmaster08.cm08statscore.ranking.RankEntry;
import net.craftmaster08.cm08statscore.resolver.UsernameResolver;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MinecraftStatProvider implements StatProvider {
    private static final Logger LOGGER = LogManager.getLogger(MinecraftStatProvider.class);

    private final MinecraftServer server;

    public MinecraftStatProvider(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public long getTotal(UUID uuid, Collection<? extends Stat<?>> stats) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            long total = 0L;
            for (Stat<?> stat : stats) {
                total += online.getStats().getValue(stat);
            }
            return total;
        }
        return readOfflineTotal(offlineStatsFile(uuid), stats);
    }

    @Override
    public long getDaily(UUID uuid, Collection<? extends Stat<?>> stats) {
        for (DailyStatsTracker tracker : StatsCore.getTrackers()) {
            if (tracker.tracks(stats)) {
                return (long) tracker.getDaily(uuid);
            }
        }
        return 0L;
    }

    @Override
    public Map<Stat<?>, Long> getTotals(UUID uuid, Collection<? extends Stat<?>> stats) {
        Map<Stat<?>, Long> result = new LinkedHashMap<>();
        for (Stat<?> stat : stats) {
            result.put(stat, getTotal(uuid, stat));
        }
        return result;
    }

    @Override
    public Map<UUID, Long> getAllTotals(Collection<? extends Stat<?>> stats) {
        Map<UUID, Long> result = new HashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            long total = 0L;
            for (Stat<?> stat : stats) {
                total += player.getStats().getValue(stat);
            }
            result.put(player.getUUID(), total);
        }

        File statsFolder = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        File[] statFiles = statsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (statFiles != null) {
            for (File statFile : statFiles) {
                UUID uuid = parseUuid(statFile);
                if (uuid == null || result.containsKey(uuid)) continue;

                long total = readOfflineTotal(statFile, stats);
                if (total > 0) {
                    result.put(uuid, total);
                }
            }
        }

        return result;
    }

    @Override
    public List<RankEntry> getLeaderboard(Collection<? extends Stat<?>> stats) {
        Map<UUID, Long> totals = getAllTotals(stats);

        List<RankEntry> entries = new ArrayList<>(totals.size());
        for (Map.Entry<UUID, Long> e : totals.entrySet()) {
            String username = UsernameResolver.resolve(server, e.getKey(), e.getKey().toString());
            entries.add(new RankEntry(e.getKey(), username, e.getValue(), 0));
        }

        entries.sort((a, b) -> {
            int cmp = Long.compare(b.value(), a.value());
            return cmp != 0 ? cmp : a.username().compareToIgnoreCase(b.username());
        });

        List<RankEntry> ranked = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            RankEntry e = entries.get(i);
            ranked.add(new RankEntry(e.uuid(), e.username(), e.value(), i + 1));
        }
        return ranked;
    }

    private File offlineStatsFile(UUID uuid) {
        File statsFolder = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        return new File(statsFolder, uuid + ".json");
    }

    private UUID parseUuid(File statFile) {
        try {
            return UUID.fromString(statFile.getName().replace(".json", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private long readOfflineTotal(File statFile, Collection<? extends Stat<?>> stats) {
        if (!statFile.exists()) return 0L;
        try (FileReader reader = new FileReader(statFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject statsObj = root.getAsJsonObject("stats");
            if (statsObj == null) return 0L;

            long total = 0L;
            for (Stat<?> stat : stats) {
                total += readStatValue(statsObj, stat);
            }
            return total;
        } catch (Exception e) {
            LOGGER.error("Failed to read stat file {}", statFile.getName(), e);
            return 0L;
        }
    }

    private <T> long readStatValue(JsonObject statsObj, Stat<T> stat) {
        ResourceLocation typeKey = BuiltInRegistries.STAT_TYPE.getKey(stat.getType());
        if (typeKey == null) return 0L;

        JsonObject typeObj = statsObj.getAsJsonObject(typeKey.toString());
        if (typeObj == null) return 0L;

        ResourceLocation valueKey = stat.getType().getRegistry().getKey(stat.getValue());
        if (valueKey == null) return 0L;

        JsonElement el = typeObj.get(valueKey.toString());
        return el != null ? el.getAsLong() : 0L;
    }
}
