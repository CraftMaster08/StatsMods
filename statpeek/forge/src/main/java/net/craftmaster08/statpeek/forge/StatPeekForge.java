package net.craftmaster08.statpeek.forge;

import net.craftmaster08.statpeek.Statpeek;
import net.minecraftforge.fml.common.Mod;

@Mod(Statpeek.MODID)
public class StatPeekForge {
    public StatPeekForge() {
        // All real setup happens in the Dist.CLIENT-gated StatPeekForgeClient below;
        // this constructor intentionally does nothing on a dedicated server.
    }
}
