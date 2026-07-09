package net.craftmaster08.cm08statscore;

import net.craftmaster08.cm08statscore.commands.CommandRegistry;
import net.craftmaster08.cm08statscore.cache.UsernameCache;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.file.Path;
import java.util.*;

@Mod(StatsCore.MODID)
public class StatsCore {
    public static final String MODID = "cm08statscore";

    private static final Logger LOGGER = LogManager.getLogger(StatsCore.class);

    private static StatProvider provider;
    private static ConfigManager configManager;
    private static UsernameCache usernameCache;
    private static MinecraftServer server;
    private static final List<DailyStatsTracker> trackers = new ArrayList<>();
    private static final Map<UUID, Long> commandCooldowns = new HashMap<>();

    public StatsCore() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        configManager = new ConfigManager();
        usernameCache = UsernameCache.getInstance(server);
        configManager.loadConfig();

        provider = new MinecraftStatProvider(server);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandRegistry.register(event.getDispatcher());
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

    public static void updateAllDailyResetTimes() {
        if (configManager == null) return;
        String time = configManager.dailyResetTime;
        for (DailyStatsTracker tracker : trackers) {
            tracker.setDailyResetTime(time);
        }
        LOGGER.info("Updated daily reset times for {} trackers", trackers.size());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null) return;

        for (DailyStatsTracker t : trackers) {
            t.getResetScheduler().checkReset();
        }

        if (server.getTickCount() % 200 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                for (DailyStatsTracker t : trackers) {
                    t.updatePlayerStat(p);
                }
            }
        }
    }
}