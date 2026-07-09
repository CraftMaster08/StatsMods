package net.craftmaster08.deathleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.stats.Stats;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod(DeathLeaderboard.MODID)
public class DeathLeaderboard {
    public static final String MODID = "deathleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(DeathLeaderboard.class);

    private static DailyStatsTracker dailyTracker;

    public DeathLeaderboard() {
        if (!ModList.get().isLoaded(StatsCore.MODID)) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling DeathLeaderboard.");
            return;
        }
        // LOWEST priority guarantees StatsCore's normal-priority ServerStartingEvent
        // listener (which loads the config) has already run by the time this fires.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
    }

    public void onServerStarting(ServerStartingEvent event) {
        ConfigManager cfg = StatsCore.getConfigManager();
        if (cfg == null) {
            LOGGER.error("StatsCore configuration not initialized; DeathLeaderboard disabled");
            return;
        }

        var deathsStat = Stats.CUSTOM.get(Stats.DEATHS);

        dailyTracker = new DailyStatsTracker(
                List.of(deathsStat),
                "daily_deaths",
                "last_known_deaths",
                1.0,
                "deaths_daily.json"
        );

        dailyTracker.setDailyResetTime(cfg.dailyResetTime);
        StatsCore.registerDailyTracker(dailyTracker);

        DeathRunCommand.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("DeathLeaderboard initialized");
    }

    public static DailyStatsTracker getDailyTracker() {
        return dailyTracker;
    }
}
