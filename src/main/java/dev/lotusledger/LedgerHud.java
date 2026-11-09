package dev.lotusledger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Uses cached text/components; rendering does not scan menus, parse chat, format counts or touch disk. */
public final class LedgerHud {
    public static final int WIDTH = 286;
    private static final int TOTAL_X = 111;
    private static final int[] COLUMNS = {148, 184, 220, 256};
    private static final Component TITLE = CozyUi.label("Lotus Ledger");
    private static final Component FROG = CozyUi.label("Frog");
    private static final Component TOTAL = CozyUi.label("Total");
    private static final List<Component> HEADERS = java.util.Arrays.stream(Tier.values()).map(tier -> CozyUi.label(tier.label())).toList();
    private LedgerHud() {}
    public record Bounds(int x, int y, int width, int height, float scale) {
        public boolean contains(double px, double py) { return px >= x && py >= y && px <= x + width && py <= y + height; }
    }
    public record RowText(Component name, Component total, List<Component> cells, HudView.Row source) {}
    public record Text(HudView view, Component summary, Component subtitle, Component first, Component second, Component empty, List<RowText> rows) {}
    public static Text prepare(HudView view) {
        List<RowText> rows = new ArrayList<>(view.rows().size());
        for (var row : view.rows()) rows.add(new RowText(CozyUi.label(row.frog().label), CozyUi.label(row.total()),
            row.cells().stream().map(CozyUi::label).toList(), row));
        return new Text(view, CozyUi.label(view.summary()), CozyUi.label(view.subtitle()), CozyUi.label(view.lineOne()),
            CozyUi.label(view.lineTwo()), CozyUi.label(view.emptyLine()), List.copyOf(rows));
    }
    public static Bounds bounds(int screenWidth, int screenHeight, LedgerData.Settings settings, int height) {
        float scale = Math.min(settings.scale, Math.min((screenWidth - 12f) / WIDTH, (screenHeight - 12f) / height));
        scale = Math.max(.1f, scale);
        int width = (int) Math.ceil(WIDTH * scale), scaledHeight = (int) Math.ceil(height * scale);
        return new Bounds(Math.clamp(settings.x, 0, Math.max(0, screenWidth - width)),
            Math.clamp(settings.y, 0, Math.max(0, screenHeight - scaledHeight)), width, scaledHeight, scale);
    }
    public static void draw(GuiGraphicsExtractor gfx, Minecraft mc, LedgerData.Settings settings, Text text) {
        var bounds = bounds(gfx.guiWidth(), gfx.guiHeight(), settings, text.view.height());
        int height = text.view.height();
        gfx.pose().pushMatrix();
        gfx.pose().translate(bounds.x, bounds.y);
        gfx.pose().scale(bounds.scale, bounds.scale);
        CozyUi.round(gfx, 1, 2, WIDTH, height, 0x42000000);
        CozyUi.round(gfx, 0, 0, WIDTH, height, 0xed17251f);
        CozyUi.round(gfx, 10, 11, 3, 11, 0xff91b69a);
        CozyUi.text(gfx, TITLE, 19, 10, 0xffe5efda);
        CozyUi.text(gfx, text.summary, WIDTH - 12 - mc.font.width(text.summary), 11, 0xffadc4ae);
        CozyUi.text(gfx, text.subtitle, 11, 26, 0xff9fb39e);
        CozyUi.text(gfx, FROG, 11, 44, 0xffb9c7b0);
        centered(gfx, mc, TOTAL, TOTAL_X, 44, 0xffc9b8d5);
        for (Tier tier : Tier.values()) centered(gfx, mc, HEADERS.get(tier.ordinal()), COLUMNS[tier.ordinal()], 44, tier.color);
        gfx.fill(10, 57, WIDTH - 10, 58, 0xff334639);
        int y = 63;
        for (int index = 0; index < text.rows.size(); index++) {
            var row = text.rows.get(index);
            if (index % 2 == 0) gfx.fill(7, y - 2, WIDTH - 7, y + 11, 0x182b4834);
            CozyUi.text(gfx, row.name, 11, y, row.source.highlighted() ? 0xffbbefae : 0xffdedfce);
            centered(gfx, mc, row.total, TOTAL_X, y, 0xffc9b8d5);
            for (Tier tier : Tier.values()) {
                int i = tier.ordinal();
                int color = row.source.collected().get(i) ? row.source.highlighted() ? 0xffbbefae : tier.color : 0xff829780;
                centered(gfx, mc, row.cells.get(i), COLUMNS[i], y, color);
            }
            y += 13;
        }
        if (text.rows.isEmpty()) { CozyUi.text(gfx, text.empty, 11, y, 0xffbbefae); y += 13; }
        gfx.fill(10, y + 2, WIDTH - 10, y + 3, 0xff334639);
        CozyUi.text(gfx, text.first, 11, y + 9, 0xffbdcdb1);
        CozyUi.text(gfx, text.second, 11, y + 20, 0xff8eaa8e);
        gfx.pose().popMatrix();
    }
    private static void centered(GuiGraphicsExtractor gfx, Minecraft mc, Component text, int x, int y, int color) {
        CozyUi.text(gfx, text, x - mc.font.width(text) / 2, y, color);
    }
}
