package net.craftmaster08.distanceleaderboard.forge;

import net.craftmaster08.distanceleaderboard.DistanceLeaderboard;
import net.minecraftforge.fml.common.Mod;

@Mod(DistanceLeaderboard.MODID)
public class DistanceLeaderboardForge {
    public DistanceLeaderboardForge() {
        DistanceLeaderboard.init();
    }
}
