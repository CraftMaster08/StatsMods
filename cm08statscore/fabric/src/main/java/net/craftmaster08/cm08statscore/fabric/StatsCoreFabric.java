package net.craftmaster08.cm08statscore.fabric;

import net.craftmaster08.cm08statscore.StatsCore;
import net.fabricmc.api.ModInitializer;

public class StatsCoreFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        StatsCore.init();
    }
}
