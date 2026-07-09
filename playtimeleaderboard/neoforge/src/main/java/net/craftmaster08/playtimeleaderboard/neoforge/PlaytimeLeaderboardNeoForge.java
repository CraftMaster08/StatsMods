package net.craftmaster08.playtimeleaderboard.neoforge;

import net.craftmaster08.playtimeleaderboard.PlaytimeLeaderboard;
import net.neoforged.fml.common.Mod;

@Mod(PlaytimeLeaderboard.MODID)
public class PlaytimeLeaderboardNeoForge {
    public PlaytimeLeaderboardNeoForge() {
        PlaytimeLeaderboard.init();
    }
}
