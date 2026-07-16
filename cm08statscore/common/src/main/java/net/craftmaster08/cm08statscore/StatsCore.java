package net.craftmaster08.cm08statscore;

import net.craftmaster08.cm08statscore.cache.UsernameCache;
import net.craftmaster08.cm08statscore.commands.CommandRegistry;
import net.craftmaster08.cm08statscore.commands.HelpEntry;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.platform.Services;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.craftmaster08.cm08statscore.statstracker.OverflowWatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.*;

public class StatsCore {
    public static final String MODID = "cm08statscore";

    private static final Logger LOGGER = LogManager.getLogger(StatsCore.class);

    private static StatProvider provider;
    private static ConfigManager configManager;
    private static UsernameCache usernameCache;
    private static MinecraftServer server;
    private static final List<DailyStatsTracker> trackers = new ArrayList<>();
    private static final List<OverflowWatcher> overflowWatchers = new ArrayList<>();
    private static final List<HelpEntry> leaderboardCommands = new ArrayList<>();
    private static final Map<UUID, Long> commandCooldowns = new HashMap<>();

    private StatsCore() {}

    /** Called once by each loader's mod entrypoint. */
    public static void init() {
        Services.PLATFORM.onServerStarting(StatsCore::onServerStarting);
        Services.PLATFORM.onEndServerTick(StatsCore::onServerTick);
    }

    private static void onServerStarting(MinecraftServer startingServer) {
        server = startingServer;
        configManager = new ConfigManager();
        usernameCache = UsernameCache.getInstance(server);
        configManager.loadConfig();

        provider = new MinecraftStatProvider(server);

        CommandRegistry.register(server.getCommands().getDispatcher());
    }

    public static StatProvider getProvider() { return provider; }
    public static void setProvider(StatProvider p) { provider = p; }
    public static ConfigManager getConfigManager() { return configManager; }
    public static UsernameCache getUsernameCache() { return usernameCache; }
    public static MinecraftServer getServer() { return server; }
    public static List<DailyStatsTracker> getTrackers() { return trackers; }

    public static Path getDailyStatsPath() {
        return server != null ? server.getWorldPath(LevelResource.ROOT).resolve("dailyStats.json") : null;
    }

    public static boolean canUseCommand(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = commandCooldowns.getOrDefault(uuid, 0L);
        long cdMs = (configManager != null ? configManager.cooldownSeconds : 5) * 1000L;
        if (now - last < cdMs) return false;
        commandCooldowns.put(uuid, now);
        return true;
    }

    public static void registerDailyTracker(DailyStatsTracker tracker) {
        trackers.add(tracker);
    }

    public static void registerOverflowWatcher(OverflowWatcher watcher) {
        overflowWatchers.add(watcher);
    }

    public static void registerLeaderboardCommand(String command, String description) {
        leaderboardCommands.add(new HelpEntry(command, description));
    }

    public static List<HelpEntry> getLeaderboardCommands() {
        return leaderboardCommands;
    }

    public static void updateAllDailyResetTimes() {
        if (configManager == null) return;
        String time = configManager.dailyResetTime;
        for (DailyStatsTracker tracker : trackers) {
            tracker.setDailyResetTime(time);
        }
        LOGGER.info("Updated daily reset times for {} trackers", trackers.size());
    }

    private static void onServerTick(MinecraftServer tickingServer) {
        if (server == null) return;

        for (DailyStatsTracker t : trackers) {
            t.getResetScheduler().checkReset();
        }

        if (server.getTickCount() % 200 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                for (OverflowWatcher w : overflowWatchers) {
                    w.checkPlayer(p);
                }
                for (DailyStatsTracker t : trackers) {
                    t.updatePlayerStat(p);
                }
            }
        }
    }
}
