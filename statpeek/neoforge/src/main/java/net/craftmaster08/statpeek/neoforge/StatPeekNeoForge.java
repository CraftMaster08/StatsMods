package net.craftmaster08.statpeek.neoforge;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.craftmaster08.statpeek.Statpeek;
import net.craftmaster08.statpeek.client.StatPeekClient;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * {@code dist = Dist.CLIENT} restricts this whole entrypoint (and everything it references,
 * transitively including {@code StatsScreen}/{@code GuiGraphics}) to client-side loading only.
 */
@Mod(value = Statpeek.MODID, dist = Dist.CLIENT)
public class StatPeekNeoForge {
    public StatPeekNeoForge() {
        Statpeek.initClient();
        NeoForge.EVENT_BUS.addListener(StatPeekNeoForge::registerCommands);
        NeoForge.EVENT_BUS.addListener(StatPeekNeoForge::onEntityInteract);
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stats")
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "player");
                            return StatPeekClient.openScreenForName(name) ? 1 : 0;
                        })));
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() && event.getTarget() instanceof Player target) {
            StatPeekClient.openScreenFor(target);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
