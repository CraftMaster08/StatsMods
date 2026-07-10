package net.craftmaster08.statpeek.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class StatsScreen extends Screen {
    private final Player targetPlayer;

    public StatsScreen(Player player) {
        super(Component.literal("Stats - " + player.getName().getString()));
        this.targetPlayer = player;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Mini player model (inventory style), rendered in a box centered on (modelX, modelY)
        int modelX = width / 4;           // left side
        int modelY = height / 2 + 40;     // lower part
        int modelScale = 30;              // size
        int modelHalfWidth = 30;
        int modelHalfHeight = 45;

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                modelX - modelHalfWidth, modelY - modelHalfHeight,
                modelX + modelHalfWidth, modelY + modelHalfHeight,
                modelScale,
                0.0625F,
                (float) mouseX, (float) mouseY,
                targetPlayer
        );

        // Title
        guiGraphics.drawCenteredString(font,
                "Stats: " + targetPlayer.getName().getString(),
                width / 2, 20, 0xFFFFFF);

        // Stats area (right side)
        int statsX = width / 2 + 20;
        int statsY = 60;
        guiGraphics.drawString(font, "Health: " + targetPlayer.getHealth() + " / " + targetPlayer.getMaxHealth(), statsX, statsY, 0xFFFFFF);
        guiGraphics.drawString(font, "Food: " + targetPlayer.getFoodData().getFoodLevel(), statsX, statsY + 12, 0xFFFFFF);
        guiGraphics.drawString(font, "XP: " + targetPlayer.totalExperience, statsX, statsY + 24, 0xFFFFFF);
        // TODO: wire playtime/other tracked stats to StatsCore's real API (e.g. DailyStatsTracker /
        // MinecraftStatProvider via a network packet to the server). The old `StatsTracker` class this
        // used to call into never existed in cm08statscore, so it was removed rather than guessed at.

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
