package net.craftmaster08.statpeek;

import com.mojang.logging.LogUtils;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.platform.Services;
import org.slf4j.Logger;

public final class Statpeek {
    public static final String MODID = "statpeek";
    private static final Logger LOGGER = LogUtils.getLogger();

    private Statpeek() {}

    /** Called once by each loader's client entrypoint. */
    public static void initClient() {
        if (!Services.PLATFORM.isModLoaded(StatsCore.MODID)) {
            LOGGER.warn("StatsCore mod (cm08statscore) not detected; StatPeek will only show vanilla stats.");
        }
        LOGGER.info("Initialized StatPeek mod");
    }
}
