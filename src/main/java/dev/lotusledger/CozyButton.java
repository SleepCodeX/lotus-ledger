package dev.lotusledger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;

public final class CozyButton extends Button {
    public CozyButton(int x, int y, int width, String label, OnPress action) {
        super(x, y, width, 22, CozyUi.label(label), action, DEFAULT_NARRATION);
    }
    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hover = isHoveredOrFocused();
        CozyUi.round(graphics, getX(), getY(), getWidth(), getHeight(), hover ? 0xff47655d : 0xff293f3a);
        var font = Minecraft.getInstance().font;
        CozyUi.text(graphics, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2, getY() + 6,
            active ? hover ? 0xfff3fff1 : 0xffdce7db : 0xff82918a);
    }
}
