package net.craftmaster08.statpeek.forge;

import net.craftmaster08.statpeek.Statpeek;
import net.craftmaster08.statpeek.client.StatPeekClient;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Guarded to {@link Dist#CLIENT}: FML skips loading/scanning this class entirely on a
 * dedicated server, so the client-only {@code StatsScreen}/{@code GuiGraphics} references it
 * pulls in (transitively, via StatPeekClient) never get classloaded server-side.
 */
@Mod.EventBusSubscriber(modid = Statpeek.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StatPeekForgeClient {

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        Statpeek.initClient();

        event.getDispatcher().register(Commands.literal("stats")
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "player");
                            return StatPeekClient.openScreenForName(name) ? 1 : 0;
                        })));
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() && event.getTarget() instanceof Player target) {
            StatPeekClient.openScreenFor(target);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
