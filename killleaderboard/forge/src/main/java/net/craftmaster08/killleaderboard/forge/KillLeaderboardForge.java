package net.craftmaster08.killleaderboard.forge;

import net.craftmaster08.killleaderboard.KillLeaderboard;
import net.minecraftforge.fml.common.Mod;

@Mod(KillLeaderboard.MODID)
public class KillLeaderboardForge {
    public KillLeaderboardForge() {
        KillLeaderboard.init();
    }
}
