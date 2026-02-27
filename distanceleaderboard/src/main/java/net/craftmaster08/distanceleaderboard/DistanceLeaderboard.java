package net.craftmaster08.distanceleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod(DistanceLeaderboard.MODID)
public class DistanceLeaderboard {
    public static final String MODID = "distanceleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(DistanceLeaderboard.class);
    private static MinecraftServer server;
    private static StatsTracker statsTracker;
    private static DailyStatsTracker dailyStatsTracker;

    private final String[] STRING_DISTANCE_STATS = {
            "walk_one_cm", "sprint_one_cm", "crouch_one_cm", "swim_one_cm",
            "fall_one_cm", "climb_one_cm", "fly_one_cm", "walk_on_water_one_cm",
            "walk_under_water_one_cm", "minecart_one_cm", "boat_one_cm", "pig_one_cm",
            "horse_one_cm", "aviate_one_cm", "strider_one_cm"
    };

    private static final List<Stat<ResourceLocation>> RESOURCE_LOCATIONS_DISTANCE_STATS = List.of(
            Stats.CUSTOM.get(Stats.WALK_ONE_CM), Stats.CUSTOM.get(Stats.SPRINT_ONE_CM),
            Stats.CUSTOM.get(Stats.CROUCH_ONE_CM), Stats.CUSTOM.get(Stats.SWIM_ONE_CM),
            Stats.CUSTOM.get(Stats.FALL_ONE_CM), Stats.CUSTOM.get(Stats.CLIMB_ONE_CM),
            Stats.CUSTOM.get(Stats.FLY_ONE_CM), Stats.CUSTOM.get(Stats.WALK_ON_WATER_ONE_CM),
            Stats.CUSTOM.get(Stats.WALK_UNDER_WATER_ONE_CM), Stats.CUSTOM.get(Stats.MINECART_ONE_CM),
            Stats.CUSTOM.get(Stats.BOAT_ONE_CM), Stats.CUSTOM.get(Stats.PIG_ONE_CM),
            Stats.CUSTOM.get(Stats.HORSE_ONE_CM), Stats.CUSTOM.get(Stats.AVIATE_ONE_CM),
            Stats.CUSTOM.get(Stats.STRIDER_ONE_CM)
    );

    public DistanceLeaderboard() {
        if (!isStatsCorePresent()) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling DistanceLeaderboard.");
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
        LOGGER.info("Initialized DistanceLeaderboard mod");
    }

    private boolean isStatsCorePresent() {
        boolean present = net.minecraftforge.fml.ModList.get().isLoaded(StatsCore.MODID);
        LOGGER.info("StatsCore present: {}", present);
        return present;
    }

    private void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        LOGGER.info("DistanceLeaderboard server set");

        if (areDependenciesReady()) {
            registerCommands(event.getServer().getCommands().getDispatcher());
            LOGGER.info("Registered /distance command during server startup");
        } else {
            LOGGER.warn("Cannot register /distance command: Dependency not fully initialized (ConfigManager: {})",
                    StatsCore.getConfigManager() != null ? "present" : "null");
        }

        statsTracker = new StatsTracker(
                server,
                "minecraft:custom",
                STRING_DISTANCE_STATS,
                RESOURCE_LOCATIONS_DISTANCE_STATS
        );

        dailyStatsTracker = new DailyStatsTracker(
                "daily_distances",
                "last_known_distances",
                "distance_daily.json",
                100.0,
                statsTracker,
                RESOURCE_LOCATIONS_DISTANCE_STATS
        );
        LOGGER.info("StatsTracker & DailyStatsTracker initialized");

        String resetTime = StatsCore.getConfigManager().dailyResetTime;
        dailyStatsTracker.setDailyResetTime(resetTime);
        StatsCore.registerDailyTracker(dailyStatsTracker);
    }

    private boolean areDependenciesReady() {
        return StatsCore.getConfigManager() != null &&
                server != null;
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        DistanceRunCommand.register(dispatcher);
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

    public static DailyStatsTracker getDailyStatsTracker() {
        if (dailyStatsTracker == null) {
            LOGGER.warn("DailyStatsTracker not initialized yet");
        }
        return dailyStatsTracker;
    }
}