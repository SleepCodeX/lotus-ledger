package dev.lotusledger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public final class CozyUi {
    private static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("lotusledger", "rounded"));
    private CozyUi() {}
    public static Component label(String text) { return Component.literal(text).withStyle(style -> style.withFont(FONT)); }
    public static void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(Minecraft.getInstance().font, text, x, y, color, false);
    }
    public static void text(GuiGraphicsExtractor graphics, String text, int x, int y, int color) { text(graphics, label(text), x, y, color); }
    public static void round(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        // A small, fixed number of flat fills. No per-frame textures, paths or blur allocation.
        graphics.fill(x + 4, y, x + width - 4, y + height, color);
        graphics.fill(x + 2, y + 1, x + width - 2, y + height - 1, color);
        graphics.fill(x + 1, y + 2, x + width - 1, y + height - 2, color);
        graphics.fill(x, y + 4, x + width, y + height - 4, color);
    }
}
