package net.craftmaster08.cm08statscore.forge;

import net.craftmaster08.cm08statscore.StatsCore;
import net.minecraftforge.fml.common.Mod;

@Mod(StatsCore.MODID)
public class StatsCoreForge {
    public StatsCoreForge() {
        StatsCore.init();
    }
}
