package net.craftmaster08.statpeek.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.craftmaster08.statpeek.Statpeek;
import net.craftmaster08.statpeek.client.StatPeekClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class StatpeekFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Statpeek.initClient();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("stats")
                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "player");
                                    return StatPeekClient.openScreenForName(name) ? 1 : 0;
                                }))));

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() && entity instanceof Player target) {
                StatPeekClient.openScreenFor(target);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
