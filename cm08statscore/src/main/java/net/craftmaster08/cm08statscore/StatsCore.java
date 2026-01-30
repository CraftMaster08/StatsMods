package net.craftmaster08.cm08statscore;

import net.craftmaster08.cm08statscore.cache.UsernameCache;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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
            LOGGER.info("StatsCore server dependencies initialized");
        }

        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (usernameCache != null) {
                    usernameCache.storeUsername(player.getUUID(), player.getGameProfile().getName());
                }
            }
        }
    }

    public static PlayerList getPlayerList() {
        return server.getPlayerList();
    }

    private static class ServiceInitializer {
        static void initialize(MinecraftServer server) {
            configManager = new ConfigManager();
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
}