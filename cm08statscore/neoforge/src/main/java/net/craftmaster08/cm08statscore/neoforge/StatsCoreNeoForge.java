package net.craftmaster08.cm08statscore.neoforge;

import net.craftmaster08.cm08statscore.StatsCore;
import net.neoforged.fml.common.Mod;

@Mod(StatsCore.MODID)
public class StatsCoreNeoForge {
    public StatsCoreNeoForge() {
        StatsCore.init();
    }
}
