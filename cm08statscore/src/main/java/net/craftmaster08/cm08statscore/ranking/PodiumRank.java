package net.craftmaster08.cm08statscore.ranking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public enum PodiumRank {
    FIRST(1, ChatFormatting.GOLD, true),
    SECOND(2, ChatFormatting.WHITE, true),
    THIRD(3, ChatFormatting.DARK_PURPLE, true),
    NONE(0, ChatFormatting.WHITE, false);

    private final int rank;
    private final ChatFormatting color;
    private final boolean isBold;

    PodiumRank(int rank, ChatFormatting color, boolean isBold) {
        this.rank = rank;
        this.color = color;
        this.isBold = isBold;
    }

    public static PodiumRank fromPosition(int position) {
        return switch (position) {
            case 1 -> FIRST;
            case 2 -> SECOND;
            case 3 -> THIRD;
            default -> NONE;
        };
    }

    public MutableComponent formatRank() {
        if (this == NONE) {
            return Component.literal("");
        }
        return Component.literal(rank + ".")
                .withStyle(Style.EMPTY.withColor(color).withBold(isBold));
    }

    public ChatFormatting getColor() {
        return color;
    }
}
