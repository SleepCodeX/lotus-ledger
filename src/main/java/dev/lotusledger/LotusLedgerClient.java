package dev.lotusledger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class LotusLedgerClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("Lotus Ledger");
    private final Minecraft mc = Minecraft.getInstance();
    private final LedgerStore store = new LedgerStore(FabricLoader.getInstance().getConfigDir().resolve("lotus-ledger.json"));
    private final FrogMenuReader menuReader = new FrogMenuReader();
    private final MenuSynchronizer menuSync = new MenuSynchronizer();
    private MenuSynchronizer.Result menuResult;
    LedgerData data;
    LedgerSession session;
    final Map<Frog, Long> highlights = new EnumMap<>(Frog.class);
    private boolean dirty, writable = true, warned, atoll, openSettings, preview;
    private int ticks, viewRevision;
    private Object lastWorld;
    private String startupWarning;
    private LedgerHud.Text cachedView;

    @Override public void onInitializeClient() {
        try {
            data = store.load();
            if (store.recoveredBackup()) startupWarning = "Recovered your last good collection backup. The damaged file was preserved.";
        } catch (IOException error) {
            LOG.error("Collection data could not be loaded; existing files will not be overwritten", error);
            data = new LedgerData();
            writable = false;
            startupWarning = "Your saved file could not be read. Progress is temporary until it is repaired; existing files are preserved.";
        }
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) observe(message);
            return true;
        });
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, overlay) -> {
            if (!overlay) observe(message);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            save();
            session = null;
            lastWorld = null;
            atoll = false;
            highlights.clear();
            closeMenu();
            invalidateView();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> save());
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("lotusledger", "collection"), (graphics, delta) -> {
                if (mc.gui.hud.isHidden() || mc.gui.screen() instanceof LedgerScreen || !data.settings.visible || !atoll) return;
                LedgerHud.draw(graphics, mc, data.settings, view());
            });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
            literal("lotusledger").executes(context -> { open(false); return 1; })
                .then(literal("settings").executes(context -> { open(false); return 1; }))
                .then(literal("preview").executes(context -> { open(true); return 1; }))
                .then(literal("toggle").executes(context -> {
                    data.settings.visible = !data.settings.visible;
                    changed();
                    feedback("Overlay " + (data.settings.visible ? "shown" : "hidden") + ". Your catches are still recorded.");
                    return 1;
                }))
        ));
    }

    private boolean onHypixel() {
        return mc.getCurrentServer() != null && !mc.hasSingleplayerServer() && GameText.isHypixel(mc.getCurrentServer().ip);
    }
    private void refreshWorld() {
        if (mc.level != lastWorld) {
            if (session != null && session.buffered() > 0)
                feedback("Your profile wasn't available before leaving. Enable Profile in /widget before fishing so catches can be saved.");
            lastWorld = mc.level;
            atoll = false;
            highlights.clear();
            closeMenu();
            if (session != null) session.bindWorld(mc.level);
            invalidateView();
        }
        if (session == null && onHypixel() && mc.player != null) {
            session = new LedgerSession(data, mc.player.getUUID().toString(), mc.getCurrentServer().ip);
            session.bindWorld(mc.level);
            invalidateView();
        }
    }
    private void observe(Component message) {
        if (!onHypixel() || mc.player == null) return;
        refreshWorld();
        consumeVerifiedGameMessage(message);
    }
    /** Processing boundary after the incoming system event's server context has been verified. */
    void consumeVerifiedGameMessage(Component message) {
        if (session == null) return;
        GameText.profile(message.getString()).ifPresent(this::selectProfile);
        GameText.parseCatch(message.getString()).ifPresent(caught -> {
            if (session.accept(message, caught)) {
                atoll = true;
                highlights.put(caught.frog(), System.nanoTime());
                changed(); // The next rendered frame uses the new counter.
                if (session.profile() != null) save(); // Catches persist immediately, even with the HUD hidden.
                else if (session.buffered() == 1)
                    feedback("Catch buffered. Enable Profile in /widget so it can be saved to your collection.");
            }
        });
    }
    private void selectProfile(String name) {
        if (session.chooseProfile(name)) {
            highlights.clear();
            closeMenu();
            changed();
            save();
        }
    }
    private void tick() {
        refreshWorld();
        if (session != null) session.nextTick();
        if (openSettings) {
            openSettings = false;
            mc.gui.setScreen(new LedgerScreen(this, preview));
        }
        ++ticks;
        if (ticks % 5 == 0) {
            long now = System.nanoTime();
            if (highlights.entrySet().removeIf(entry -> now - entry.getValue() >= 5_000_000_000L)) invalidateView();
            if (onHypixel() && mc.player != null && session != null
                && mc.gui.screen() instanceof AbstractContainerScreen<?> screen
                && GameText.clean(screen.getTitle().getString()).equals("Trophy Frogs")) synchronizeMenu(screen);
            else closeMenu();
        }
        if (ticks % 10 == 0 && onHypixel() && mc.player != null && mc.getConnection() != null && session != null) {
            for (var info : mc.getConnection().getOnlinePlayers()) {
                Component display = info.getTabListDisplayName();
                if (display == null) continue;
                String line = GameText.clean(display.getString());
                if (line.startsWith("Area: ")) atoll = line.equals("Area: Lotus Atoll");
                GameText.profile(line).ifPresent(this::selectProfile);
            }
            if (startupWarning != null) { feedback(startupWarning); startupWarning = null; }
        }
        if (dirty && ticks % 20 == 0) save(); // Debounce layout edits; catches and menu changes save immediately.
    }
    /** Called only for a verified collector menu. Kept separate from network context for production-client tests. */
    void synchronizeMenu(AbstractContainerScreen<?> screen) {
        if (session == null || progress() == null || mc.player == null
            || !GameText.clean(screen.getTitle().getString()).equals("Trophy Frogs")) return;
        atoll = true;
        var snapshot = menuReader.read(screen.getMenu(), mc.player.getInventory());
        var result = menuSync.observe(screen.getMenu(), progress(), session.catchRevision(), snapshot);
        if (!result.equals(menuResult)) { menuResult = result; invalidateView(); }
        if (result.changedSpecies() > 0) {
            changed();
            save();
        }
    }
    private void closeMenu() {
        menuReader.clear();
        menuSync.close();
        if (menuResult != null) { menuResult = null; invalidateView(); }
    }
    LedgerData.Progress progress() { return session == null ? null : session.progress(); }
    String profileLabel() {
        if (session == null || session.profile() == null) return "Waiting for profile";
        String profile = session.profile();
        return Character.toUpperCase(profile.charAt(0)) + profile.substring(1);
    }
    int viewRevision() { return viewRevision; }
    LedgerHud.Text view() {
        if (cachedView == null) cachedView = LedgerHud.prepare(HudView.build(data.settings, progress(), profileLabel(),
            highlights.isEmpty() ? EnumSet.noneOf(Frog.class) : EnumSet.copyOf(highlights.keySet()), false, menuResult));
        return cachedView;
    }
    private void invalidateView() { cachedView = null; viewRevision++; }
    void open(boolean sample) { preview = sample; openSettings = true; }
    void changed() { dirty = true; invalidateView(); }
    void layoutChanged() { dirty = true; }
    void save() {
        if (!dirty || !writable) return;
        try { store.save(data); dirty = false; warned = false; }
        catch (IOException | IllegalArgumentException error) {
            LOG.error("Could not save Lotus Ledger; retaining the in-memory collection", error);
            if (!warned) { feedback("Progress couldn't be saved. Your in-memory collection is retained; check the game log and disk space."); warned = true; }
        }
    }
    private void feedback(String message) {
        if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(Component.literal("§2[Lotus Ledger] §f" + message));
    }
}
