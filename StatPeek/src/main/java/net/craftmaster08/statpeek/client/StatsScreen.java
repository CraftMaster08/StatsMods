package net.craftmaster08.statpeek.client;

import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.craftmaster08.statpeek.Statpeek;
import net.craftmaster08.statpeek.fetch.StatFetcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class StatsScreen extends Screen {
    private final Player targetPlayer;
    private final StatFetcher statFetcher = new StatFetcher();

    public StatsScreen(Player player) {
        super(Component.literal("Stats - " + player.getName().getString()));
        this.targetPlayer = player;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        // Mini player model (inventory style)
        int modelX = width / 4;           // left side
        int modelY = height / 2 + 40;     // lower part
        int modelScale = 30;              // size


        double playtime = statFetcher.getPlaytimeAsDouble(Statpeek.getPlaytimeTracker() ,targetPlayer) / 20;

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                modelX, modelY,
                modelScale,
                modelX - mouseX,           // mouse follow rotation
                modelY - mouseY,
                targetPlayer
        );

        // Title
        guiGraphics.drawCenteredString(font,
                "Stats: " + targetPlayer.getName().getString(),
                width / 2, 20, 0xFFFFFF);

        // Example stats area (right side) - replace later with StatsCore data
        int statsX = width / 2 + 20;
        int statsY = 60;
        guiGraphics.drawString(font, "Health: " + targetPlayer.getHealth() + " / " + targetPlayer.getMaxHealth(), statsX, statsY, 0xFFFFFF);
        guiGraphics.drawString(font, "Food: " + targetPlayer.getFoodData().getFoodLevel(), statsX, statsY + 12, 0xFFFFFF);
        guiGraphics.drawString(font, "XP: " + targetPlayer.totalExperience, statsX, statsY + 24, 0xFFFFFF);
        guiGraphics.drawString(font, "Playtime: " + playtime, statsX, statsY + 36, 0xFFFFFF);

        // Add more stats/icons here when integrating StatsCore

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}