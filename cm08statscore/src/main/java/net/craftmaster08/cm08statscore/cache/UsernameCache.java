package net.craftmaster08.cm08statscore.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UsernameCache {
    private static final Logger LOGGER = LogManager.getLogger(UsernameCache.class);

    private static class Holder {
        private static volatile UsernameCache INSTANCE;
    }

    private File cacheFile;
    private Map<UUID, String> usernameMap;
    private Gson gson;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private UsernameCache(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("MinecraftServer cannot be null");
        }
        this.cacheFile = new File(server.getServerDirectory(), "statsconfig_username_cache.json");
        this.usernameMap = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadCache();
    }

    public static UsernameCache getInstance(MinecraftServer server) {
        UsernameCache instance = Holder.INSTANCE;
        if (instance == null) {
            synchronized (UsernameCache.class) {
                instance = Holder.INSTANCE;
                if (instance == null) {
                    instance = new UsernameCache(server);
                    Holder.INSTANCE = instance;
                }
            }
        }
        return instance;
    }

    public String getUsername(UUID uuid) {
        lock.readLock().lock();
        try {
            return usernameMap.get(uuid);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void storeUsername(UUID uuid, String username) {
        lock.writeLock().lock();
        try {
            usernameMap.put(uuid, username);
            saveCache();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadCache() {
        if (!cacheFile.exists()) {
            return;
        }
        lock.writeLock().lock();
        try (FileReader reader = new FileReader(cacheFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> rawMap = gson.fromJson(reader, type);
            if (rawMap != null) {
                for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        usernameMap.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid UUID in cache: {}", entry.getKey());
                    }
                }
                LOGGER.info("Loaded username cache with {} entries", usernameMap.size());
            }
        } catch (IOException e) {
            LOGGER.error("Error loading username cache", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void saveCache() {
        lock.writeLock().lock();
        try {
            Map<String, String> rawMap = new HashMap<>();
            usernameMap.forEach((uuid, username) -> rawMap.put(uuid.toString(), username));
            try (FileWriter writer = new FileWriter(cacheFile)) {
                gson.toJson(rawMap, writer);
                LOGGER.info("Saved username cache");
            }
        } catch (IOException e) {
            LOGGER.error("Error saving username cache", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}