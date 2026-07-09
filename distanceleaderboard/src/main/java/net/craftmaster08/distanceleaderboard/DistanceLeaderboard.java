package net.craftmaster08.distanceleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod(DistanceLeaderboard.MODID)
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

    public DistanceLeaderboard() {
        if (!ModList.get().isLoaded(StatsCore.MODID)) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling DistanceLeaderboard.");
            return;
        }
        // LOWEST priority guarantees StatsCore's normal-priority ServerStartingEvent
        // listener (which loads the config) has already run by the time this fires.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
    }

    public void onServerStarting(ServerStartingEvent event) {
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

        DistanceRunCommand.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("DistanceLeaderboard initialized");
    }

    public static DailyStatsTracker getDailyTracker() {
        return dailyTracker;
    }
}
