package net.craftmaster08.cm08statscore;

import net.craftmaster08.cm08statscore.cache.UsernameCache;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(StatsCore.MODID)
public class StatsCore {
    public static final String MODID = "cm08statscore";
    private static final Logger LOGGER = LogManager.getLogger(StatsCore.class);
    private static ConfigManager configManager;
    private static UsernameCache usernameCache;
    private static DailyStatsTracker dailyStatsTracker;
    private static MinecraftServer server;

    public StatsCore() {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        LOGGER.info("Initialized StatsCore mod");
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        CommandRegistry.register(event.getDispatcher());
        LOGGER.info("Registered StatsCore commands");
    }

    private static class EventHandler {
        @SubscribeEvent(priority = EventPriority.LOW)
        public void onServerStarting(ServerStartingEvent event) {
            server = event.getServer();
            ServiceInitializer.initialize(server);
            LOGGER.info("StatsCore server dependencies initialized with {} username colors and {} blacklisted players",
                    configManager.getUsernameColors().size(), configManager.getBlacklistedPlayers().size());
        }

        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
                if (player.tickCount % 100 == 0 && dailyStatsTracker != null) {
                    dailyStatsTracker.updatePlayerPlaytime(player);
                    dailyStatsTracker.updatePlayerDistance(player);
                }
            }
        }

        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (dailyStatsTracker != null) {
                    dailyStatsTracker.playerLoggedIn(player);
                }
                if (usernameCache != null) {
                    usernameCache.storeUsername(player.getUUID(), player.getGameProfile().getName());
                }
            }
        }

        @SubscribeEvent
        public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player && dailyStatsTracker != null) {
                dailyStatsTracker.playerLoggedOut(player);
            }
        }
    }

    private static class ServiceInitializer {
        static void initialize(MinecraftServer server) {
            try {
                dailyStatsTracker = new DailyStatsTracker(server);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to initialize DailyStatsTracker: {}", e.getMessage(), e);
                dailyStatsTracker = null;
            }
            configManager = new ConfigManager(dailyStatsTracker);
            usernameCache = UsernameCache.getInstance(server);
            try {
                configManager.loadConfig();
            } catch (Exception e) {
                LOGGER.error("Failed to load config: {}", e.getMessage(), e);
            }
        }
    }

    public static ConfigManager getConfigManager() {
        if (configManager == null) {
            LOGGER.warn("ConfigManager accessed before initialization");
        }
        return configManager;
    }

    public static DailyStatsTracker getDailyStatsTracker() {
        if (dailyStatsTracker == null) {
            LOGGER.warn("DailyStatsTracker accessed but is null");
        }
        return dailyStatsTracker;
    }
}