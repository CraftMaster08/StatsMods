package net.craftmaster08.deathleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod(DeathLeaderboard.MODID)
public class DeathLeaderboard {
    public static final String MODID = "deathleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(DeathLeaderboard.class);
    private static MinecraftServer server;
    private static StatsTracker statsTracker;
    private static StatsTracker playtimeTracker;
    private static DailyStatsTracker dailyStatsTracker;

    public DeathLeaderboard() {
        if (!isStatsCorePresent()) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling DeathLeaderboard.");
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
        LOGGER.info("Initialized DeathLeaderboard mod");
    }

    private boolean isStatsCorePresent() {
        boolean present = net.minecraftforge.fml.ModList.get().isLoaded(StatsCore.MODID);
        LOGGER.info("StatsCore present: {}", present);
        return present;
    }

    private void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        LOGGER.info("DeathLeaderboard server set");

        if (areDependenciesReady()) {
            registerCommands(event.getServer().getCommands().getDispatcher());
            LOGGER.info("Registered /deaths command during server startup");
        } else {
            LOGGER.warn("Cannot register /deaths command: Dependencies not fully initialized (ConfigManager: {})",
                    StatsCore.getConfigManager() != null ? "present" : "null");
        }

        statsTracker = new StatsTracker(
                server,
                "minecraft:custom",
                "deaths",
                Stats.CUSTOM.get(Stats.DEATHS)
        );

        playtimeTracker = new StatsTracker(
                server,
                "minecraft:custom",
                "play_time",
                Stats.CUSTOM.get(Stats.PLAY_TIME)
        );

        dailyStatsTracker = new DailyStatsTracker(
                Path.of("deaths_daily.json"),
                "daily_deaths",
                statsTracker,
                Stats.CUSTOM.get(Stats.DEATHS)
        );

        LOGGER.info("StatsTrackers & DailyStatsTracker initialized");
        StatsCore.registerDailyTracker(dailyStatsTracker);
    }

    private boolean areDependenciesReady() {
        return StatsCore.getConfigManager() != null &&
                server != null;
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        DeathRunCommand.register(dispatcher);
    }

    public static MinecraftServer getServer() {
        if (server == null) {
            LOGGER.warn("Attempted to access server before initialization");
        }
        return server;
    }

    public static StatsTracker getStatsTracker() {
        if (statsTracker == null) {
            LOGGER.warn("StatsTracker not initialized yet");
        }
        return statsTracker;
    }

    public static StatsTracker getPlaytimeTracker() {
        if (playtimeTracker == null) {
            LOGGER.warn("PlaytimeTracker not initialized yet");
        }
        return playtimeTracker;
    }

    public static DailyStatsTracker getDailyStatsTracker() {
        if (dailyStatsTracker == null) {
            LOGGER.warn("DailyStatsTracker not initialized yet");
        }
        return dailyStatsTracker;
    }
}