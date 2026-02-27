package net.craftmaster08.playtimeleaderboard;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public enum HourRange {
    UNDER_100(0, 100, new ChatFormatting[]{ChatFormatting.GRAY}, ChatFormatting.GRAY, null, null),
    H_100_199(100, 200, new ChatFormatting[]{ChatFormatting.WHITE}, ChatFormatting.WHITE, null, null),
    H_200_299(200, 300, new ChatFormatting[]{ChatFormatting.GOLD}, ChatFormatting.GOLD, null, null),
    H_300_399(300, 400, new ChatFormatting[]{ChatFormatting.AQUA}, ChatFormatting.AQUA, null, null),
    H_400_499(400, 500, new ChatFormatting[]{ChatFormatting.DARK_GREEN}, ChatFormatting.DARK_GREEN, null, null),
    H_500_599(500, 600, new ChatFormatting[]{ChatFormatting.DARK_AQUA}, ChatFormatting.DARK_AQUA, null, null),
    H_600_699(600, 700, new ChatFormatting[]{ChatFormatting.DARK_RED}, ChatFormatting.DARK_RED, null, null),
    H_700_799(700, 800, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, ChatFormatting.LIGHT_PURPLE, null, null),
    H_800_899(800, 900, new ChatFormatting[]{ChatFormatting.BLUE}, ChatFormatting.BLUE, null, null),
    H_900_999(900, 1000, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, ChatFormatting.DARK_PURPLE, null, null),
    H_1000_1099(1000, 1100, new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.AQUA}, ChatFormatting.LIGHT_PURPLE, "✫", ChatFormatting.RED),
    H_1100_1199(1100, 1200, new ChatFormatting[]{ChatFormatting.WHITE}, ChatFormatting.GRAY, "✪", ChatFormatting.GRAY),
    H_1200_1299(1200, 1300, new ChatFormatting[]{ChatFormatting.YELLOW}, ChatFormatting.GRAY, "✪", ChatFormatting.GOLD),
    H_1300_1399(1300, 1400, new ChatFormatting[]{ChatFormatting.AQUA}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_AQUA),
    H_1400_1499(1400, 1500, new ChatFormatting[]{ChatFormatting.GREEN}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_GREEN),
    H_1500_1599(1500, 1600, new ChatFormatting[]{ChatFormatting.DARK_AQUA}, ChatFormatting.GRAY, "✪", ChatFormatting.BLUE),
    H_1600_1699(1600, 1700, new ChatFormatting[]{ChatFormatting.RED}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_RED),
    H_1700_1799(1700, 1800, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_PURPLE),
    H_1800_1899(1800, 1900, new ChatFormatting[]{ChatFormatting.BLUE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_BLUE),
    H_1900_1999(1900, 2000, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_GRAY),
    H_2000_2099(2000, 2100, new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.GRAY}, ChatFormatting.DARK_GRAY, "✪", ChatFormatting.GRAY);

    private final double minHours;
    private final double maxHours;
    private final ChatFormatting[] hoursColors;
    private final ChatFormatting hColor;
    private final String starSymbol;
    private final ChatFormatting starColor;

    HourRange(double minHours, double maxHours, ChatFormatting[] hoursColors, ChatFormatting hColor, String starSymbol, ChatFormatting starColor) {
        this.minHours = minHours;
        this.maxHours = maxHours;
        this.hoursColors = hoursColors;
        this.hColor = hColor;
        this.starSymbol = starSymbol;
        this.starColor = starColor;
    }

    public static HourRange findRange(double playtime) {
        for (HourRange range : values()) {
            if (playtime >= range.minHours && playtime < range.maxHours) {
                return range;
            }
        }
        return H_2000_2099; //change to unter 100
    }

    public MutableComponent formatHours(double playtime) {
        String hoursText = playtime >= 1000.0 ? String.format("%d", (int) playtime) : String.format("%.2f", playtime);
        MutableComponent component = Component.literal("");

        if (starSymbol != null) {
            component.append(Component.literal(starSymbol + " ")
                    .withStyle(Style.EMPTY.withColor(starColor).withBold(false)));
        }

        if (hoursColors.length > 1) {
            String[] chars = hoursText.split("");
            for (int i = 0; i < chars.length; i++) {
                ChatFormatting color = i < hoursColors.length ? hoursColors[i] : ChatFormatting.WHITE;
                component.append(Component.literal(chars[i])
                        .withStyle(Style.EMPTY.withColor(color).withBold(false)));
            }
        } else {
            component.append(Component.literal(hoursText)
                    .withStyle(Style.EMPTY.withColor(hoursColors[0]).withBold(false)));
        }

        component.append(Component.literal("h")
                .withStyle(Style.EMPTY.withColor(hColor).withBold(false)));

        return component;
    }
}
