package net.craftmaster08.killleaderboard.neoforge;

import net.craftmaster08.killleaderboard.KillLeaderboard;
import net.neoforged.fml.common.Mod;

@Mod(KillLeaderboard.MODID)
public class KillLeaderboardNeoForge {
    public KillLeaderboardNeoForge() {
        KillLeaderboard.init();
    }
}
