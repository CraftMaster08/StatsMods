package net.craftmaster08.statpeek;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.craftmaster08.statpeek.client.StatsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;

@Mod(Statpeek.MODID)
public class Statpeek {
    public static final String MODID = "statpeek";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer server;
    //public static List<StatsTracker> trackers;
    private static StatsTracker playtimeTracker;

    public Statpeek() {
        if (!isStatsCorePresent()) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected.");
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
        LOGGER.info("Initialized StatPeek mod");
    }

    private boolean isStatsCorePresent() {
        boolean present = net.minecraftforge.fml.ModList.get().isLoaded(StatsCore.MODID);
        LOGGER.info("StatsCore present: {}", present);
        return present;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        LOGGER.info("StatPeek server set");

        playtimeTracker = new StatsTracker(server, "minecraft:custom", "play_time", Stats.CUSTOM.get(Stats.PLAY_TIME));
        //StatsTracker distanceTracker = new StatsTracker(server, "minecraft:custom", "play_time", Stats.CUSTOM.get(Stats.PLAY_TIME));
        //StatsTracker deathTracker = new StatsTracker(server, "minecraft:custom", "deaths", Stats.CUSTOM.get(Stats.DEATHS));
        //StatsTracker killTracker = new StatsTracker(server, "minecraft:custom", "player_kills", Stats.CUSTOM.get(Stats.PLAYER_KILLS));
        //StatsTracker jumpTracker = new StatsTracker(server, "minecraft:custom", "jump", Stats.CUSTOM.get(Stats.JUMP));

        //trackers.add(playtimeTracker);
        //trackers.add(deathTracker);
        //trackers.add(killTracker);
        //trackers.add(jumpTracker);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerCommands(RegisterClientCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
            dispatcher.register(Commands.literal("stats")
                    .then(Commands.argument("player", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                Minecraft mc = Minecraft.getInstance();
                                Player target = mc.level.players().stream()
                                        .filter(p -> p.getName().getString().equalsIgnoreCase(name))
                                        .findFirst().orElse(null);
                                if (target != null) {
                                    mc.setScreen(new StatsScreen(target));
                                    return 1;
                                }
                                return 0;
                            })));
        }
    }

    public static class ClientEvents {
        @SubscribeEvent
        public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            if (event.getLevel().isClientSide() && event.getTarget() instanceof Player target) {
                Minecraft.getInstance().setScreen(new StatsScreen(target));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    public static StatsTracker getPlaytimeTracker() {
        return playtimeTracker;
    }
}