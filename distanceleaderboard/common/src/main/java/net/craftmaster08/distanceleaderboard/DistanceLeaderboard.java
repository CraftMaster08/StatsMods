package net.craftmaster08.distanceleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.platform.Services;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DistanceLeaderboard {
    public static final String MODID = "distanceleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(DistanceLeaderboard.class);

    public static final List<Stat<ResourceLocation>> DISTANCE_STATS = List.of(
            Stats.CUSTOM.get(Stats.WALK_ONE_CM), Stats.CUSTOM.get(Stats.SPRINT_ONE_CM),
            Stats.CUSTOM.get(Stats.CROUCH_ONE_CM), Stats.CUSTOM.get(Stats.SWIM_ONE_CM),
            Stats.CUSTOM.get(Stats.FALL_ONE_CM), Stats.CUSTOM.get(Stats.CLIMB_ONE_CM),
            Stats.CUSTOM.get(Stats.FLY_ONE_CM), Stats.CUSTOM.get(Stats.WALK_ON_WATER_ONE_CM),
            Stats.CUSTOM.get(Stats.WALK_UNDER_WATER_ONE_CM), Stats.CUSTOM.get(Stats.MINECART_ONE_CM),
            Stats.CUSTOM.get(Stats.BOAT_ONE_CM), Stats.CUSTOM.get(Stats.PIG_ONE_CM),
            Stats.CUSTOM.get(Stats.HORSE_ONE_CM), Stats.CUSTOM.get(Stats.AVIATE_ONE_CM),
            Stats.CUSTOM.get(Stats.STRIDER_ONE_CM)
    );

    private static DailyStatsTracker dailyTracker;

    private DistanceLeaderboard() {}

    /** Called once by each loader's mod entrypoint. */
    public static void init() {
        if (!Services.PLATFORM.isModLoaded(StatsCore.MODID)) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling DistanceLeaderboard.");
            return;
        }
        Services.PLATFORM.onServerStartingLate(DistanceLeaderboard::onServerStarting);
    }

    private static void onServerStarting(MinecraftServer server) {
        ConfigManager cfg = StatsCore.getConfigManager();
        if (cfg == null) {
            LOGGER.error("StatsCore configuration not initialized; DistanceLeaderboard disabled");
            return;
        }

        dailyTracker = new DailyStatsTracker(
                DISTANCE_STATS,
                "daily_distances",
                "last_known_distances",
                1.0,
                "distance_daily.json"
        );

        dailyTracker.setDailyResetTime(cfg.dailyResetTime);
        StatsCore.registerDailyTracker(dailyTracker);

        DistanceRunCommand.register(server.getCommands().getDispatcher());
        LOGGER.info("DistanceLeaderboard initialized");
    }

    public static DailyStatsTracker getDailyTracker() {
        return dailyTracker;
    }
}
