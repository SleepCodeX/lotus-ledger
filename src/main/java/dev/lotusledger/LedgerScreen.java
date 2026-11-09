package dev.lotusledger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.Set;

public final class LedgerScreen extends Screen {
    private static final Component HEADING = CozyUi.label("Lotus Ledger");
    private static final Component HINT = CozyUi.label("Drag the card to make it feel at home.");
    private static final Component SETTINGS = CozyUi.label("Display & layout");
    private final LotusLedgerClient owner;
    private final boolean preview;
    private final LedgerData.Progress sample;
    private LedgerHud.Text previewText;
    private int previewRevision = -1;
    private boolean dragging;
    private double offsetX, offsetY;
    public LedgerScreen(LotusLedgerClient owner, boolean preview) {
        super(Component.literal("Lotus Ledger"));
        this.owner = owner;
        this.preview = preview;
        sample = new LedgerData.Progress();
        for (Frog frog : Frog.values()) sample.synchronize(frog, FrogMenu.parse(java.util.List.of("Undiscovered")).orElseThrow());
        for (Tier tier : Tier.values()) sample.record(new GameText.Catch(Frog.COMMON_FROG, tier));
        sample.record(new GameText.Catch(Frog.CAVE_FROG, Tier.GOLD));
        sample.record(new GameText.Catch(Frog.PUDDLE_JUMPER, Tier.DIAMOND));
    }
    private LedgerHud.Text shownText() {
        if (!preview) return owner.view();
        if (previewText == null || previewRevision != owner.viewRevision()) {
            previewRevision = owner.viewRevision();
            previewText = LedgerHud.prepare(HudView.build(owner.data.settings, sample, "Preview", Set.of(), true, null));
        }
        return previewText;
    }
    @Override protected void init() {
        var settings = owner.data.settings;
        int x = Math.max(6, width - 180), y = 43;
        addRenderableWidget(new CozyButton(x, y, 168, settings.visible ? "Overlay: Visible" : "Overlay: Hidden", button -> {
            settings.visible = !settings.visible; changed();
        }));
        addRenderableWidget(new CozyButton(x, y += 27, 168, settings.counts ? "View: Counts" : "View: Checkmarks", button -> {
            settings.counts = !settings.counts; changed();
        }));
        String filter = settings.missingTier < 0 ? "Missing: Show all" : "Missing: " + Tier.values()[settings.missingTier].label();
        addRenderableWidget(new CozyButton(x, y += 27, 168, filter, button -> {
            settings.missingTier = settings.missingTier == 3 ? -1 : settings.missingTier + 1; changed();
        }));
        addRenderableWidget(new CozyButton(x, y += 27, 80, "Smaller", button -> {
            settings.scale = Math.max(.6f, settings.scale - .1f); changed();
        }));
        addRenderableWidget(new CozyButton(x + 88, y, 80, "Larger", button -> {
            settings.scale = Math.min(2f, settings.scale + .1f); changed();
        }));
        addRenderableWidget(new CozyButton(x, y += 27, 168, "Reset layout", button -> {
            settings.x = 18; settings.y = 60; settings.scale = 1; changed();
        }));
        addRenderableWidget(new CozyButton(x, y += 27, 168, preview ? "Show my collection" : "Preview sample", button -> {
            Minecraft.getInstance().gui.setScreen(new LedgerScreen(owner, !preview));
        }));
        addRenderableWidget(new CozyButton(x, y + 32, 168, "Done", button -> onClose()));
    }
    private void changed() { owner.changed(); rebuildWidgets(); }
    @Override public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(0, 0, width, height, 0xcc101a16);
        CozyUi.text(gfx, HEADING, 14, 10, 0xffe4edda);
        CozyUi.text(gfx, HINT, 14, 26, 0xff9eb59a);
        CozyUi.text(gfx, SETTINGS, Math.max(6, width - 180), 22, 0xffbfd1b8);
        LedgerHud.draw(gfx, Minecraft.getInstance(), owner.data.settings, shownText());
        super.extractRenderState(gfx, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) return true;
        var bounds = LedgerHud.bounds(width, height, owner.data.settings, shownText().view().height());
        if (event.button() == 0 && bounds.contains(event.x(), event.y())) {
            dragging = true;
            offsetX = event.x() - bounds.x();
            offsetY = event.y() - bounds.y();
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        owner.data.settings.x = Math.max(0, (int) (event.x() - offsetX));
        owner.data.settings.y = Math.max(0, (int) (event.y() - offsetY));
        owner.layoutChanged();
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { dragging = false; return super.mouseReleased(event); }
    @Override public void onClose() { owner.save(); Minecraft.getInstance().gui.setScreen(null); }
    @Override public boolean isPauseScreen() { return false; }
}
