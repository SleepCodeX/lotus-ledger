package dev.lotusledger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import java.util.ArrayList;
import java.util.List;

/** Exercises the actual production jar in a disposable offline client, without touching an installed instance. */
public final class LedgerClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var loader = FabricLoader.getInstance();
        require(loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("26.2"), "Minecraft 26.2");
        require(loader.getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("0.19.5"), "Fabric Loader 0.19.5");
        require(!loader.isModLoaded("skyhanni"), "Standalone startup without SkyHanni");
        var owner = loader.getEntrypoints("client", ClientModInitializer.class).stream()
            .filter(LotusLedgerClient.class::isInstance).map(LotusLedgerClient.class::cast).findFirst().orElseThrow();
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(mc -> {
            owner.data = new LedgerData(); // Each run uses fresh data in this disposable client directory.
            require(mc.font.width(CozyUi.label("iiii")) < mc.font.width(CozyUi.label("WWWW")), "Rounded font loaded real proportional glyphs");
            mc.options.guiScale().set(2);
            mc.gui.setScreen(new LedgerScreen(owner, true));
        });
        context.waitTicks(5);
        context.takeScreenshot("lotus-ledger-preview");
        context.runOnClient(mc -> {
            require(mc.gui.screen() instanceof LedgerScreen, "Settings screen is open");
            var screen = (LedgerScreen) mc.gui.screen();
            require(owner.data.profiles.isEmpty(), "Preview creates no saved collection");
            Button countButton = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().startsWith("View:")).findFirst().orElseThrow();
            screen.mouseClicked(new MouseButtonEvent(countButton.getX() + 2, countButton.getY() + 2, new MouseButtonInfo(0, 0)), false);
            require(owner.data.settings.counts, "Count-view button works");
            int oldX = owner.data.settings.x, oldY = owner.data.settings.y;
            require(screen.mouseClicked(new MouseButtonEvent(oldX + 40, oldY + 6, new MouseButtonInfo(0, 0)), false), "Overlay captures drag");
            screen.mouseDragged(new MouseButtonEvent(oldX + 80, oldY + 26, new MouseButtonInfo(0, 0)), 40, 20);
            screen.mouseReleased(new MouseButtonEvent(oldX + 80, oldY + 26, new MouseButtonInfo(0, 0)));
            require(owner.data.settings.x == oldX + 40 && owner.data.settings.y == oldY + 20, "Drag updates position");
        });
        context.waitTicks(3);
        context.takeScreenshot("lotus-ledger-counts");
        context.runOnClient(mc -> {
            mc.gui.screen().onClose();
            require(!(mc.gui.screen() instanceof LedgerScreen), "Editor closes normally (title screen when no world is open)");
            require(owner.data.profiles.isEmpty(), "Preview leaves live collection untouched");
            try {
                var saved = new LedgerStore(loader.getConfigDir().resolve("lotus-ledger.json")).load();
                require(saved.settings.counts && saved.settings.x == owner.data.settings.x, "Layout and view survive reload");
                require(saved.profiles.isEmpty(), "Only settings, no sample catches, were persisted");
            } catch (java.io.IOException error) { throw new AssertionError("Saved settings", error); }
        });
        checkCollectorAndCatches(context, owner);
        System.out.println("PASS: standalone startup, collector item/lore import, upward/downward refresh, idempotence, chat counter, displayed totals, immediate save/reload, profile isolation, cached parsing, preview and layout controls");
    }
    private static void checkCollectorAndCatches(ClientGameTestContext context, LotusLedgerClient owner) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(mc -> {
                owner.data = new LedgerData();
                owner.data.settings.counts = true;
                owner.session = new LedgerSession(owner.data, "client-test", "mc.hypixel.net");
                owner.session.bindWorld(mc.level);
                owner.session.chooseProfile("lime");
                owner.changed();
                mc.gui.setScreen(new LedgerScreen(owner, false));
            });
            context.waitTicks(3);
            context.takeScreenshot("lotus-ledger-first-visit");
            context.runOnClient(mc -> {
                // Controlled server input in a disposable local world; no live Hypixel connection.
                ContainerScreen screen = collector(mc, 1, 24);
                mc.gui.setScreen(screen);
                var reader = new FrogMenuReader();
                var parsed = reader.read(screen.getMenu(), mc.player.getInventory());
                require(parsed.size() == 12, "All 12 collector entries parsed from actual ItemLore components");
                int parses = reader.parseCount();
                reader.read(screen.getMenu(), mc.player.getInventory());
                require(reader.parseCount() == parses, "Unchanged item components reuse parsed results");
                owner.synchronizeMenu(screen);
                owner.synchronizeMenu(screen);
                require(owner.progress().initializedSpecies() == 12, "Menu initializes every species");
                require(owner.progress().get(Frog.COMMON_FROG).counts[0] == 24, "Player inventory lookalikes are excluded");
                var packet = Component.literal("§6 §lTROPHY FROG! §fYou caught §fCommon Frog §8§lBRONZE§f!");
                owner.consumeVerifiedGameMessage(packet);
                owner.consumeVerifiedGameMessage(packet);
                owner.consumeVerifiedGameMessage(Component.literal("Party > Player:  TROPHY FROG! You caught Common Frog BRONZE!"));
                require(owner.progress().get(Frog.COMMON_FROG).counts[0] == 25, "One incoming catch immediately increments exactly once");
                var view = owner.view().view();
                var row = view.rows().stream().filter(value -> value.frog() == Frog.COMMON_FROG).findFirst().orElseThrow();
                require(row.cells().getFirst().equals("25") && row.total().equals("27"), "Visible tier and species totals reflect the catch");
                require(view.summary().equals("4 / 48 tiers") && view.subtitle().equals("Lime · 32 catches"), "Collection tiers and catch total are distinct and correct");
                require(owner.view() == owner.view(), "Unchanged frames reuse the prepared HUD");
                var saved = new LedgerStore(FabricLoader.getInstance().getConfigDir().resolve("lotus-ledger.json")).load();
                require(saved.profiles.get("client-test/main/lime").get(Frog.COMMON_FROG).counts[0] == 25, "Incoming catch was immediately persisted");
                mc.gui.setScreen(new LedgerScreen(owner, false));
            });
            context.waitTicks(3);
            context.takeScreenshot("lotus-ledger-live-counter");
            context.runOnClient(mc -> {
                var lower = collector(mc, 2, 7);
                mc.gui.setScreen(lower);
                owner.synchronizeMenu(lower); owner.synchronizeMenu(lower);
                require(owner.progress().get(Frog.COMMON_FROG).counts[0] == 7, "Reopened collector corrects a counter downward");
                var unchanged = collector(mc, 3, 7);
                mc.gui.setScreen(unchanged);
                owner.synchronizeMenu(unchanged); owner.synchronizeMenu(unchanged);
                require(owner.progress().get(Frog.COMMON_FROG).counts[0] == 7, "Unchanged menu reopening is idempotent");
                var higher = collector(mc, 4, 30);
                mc.gui.setScreen(higher);
                owner.synchronizeMenu(higher); owner.synchronizeMenu(higher);
                require(owner.progress().get(Frog.COMMON_FROG).counts[0] == 30, "Reopened collector corrects a counter upward");
                owner.consumeVerifiedGameMessage(Component.literal(" TROPHY FROG! You caught Common Frog BRONZE!"));
                require(owner.progress().get(Frog.COMMON_FROG).counts[0] == 31, "The next catch increments from the reconciled baseline");
                owner.session.chooseProfile("pear"); owner.changed();
                require(owner.progress().totalCatches() == 0, "Second profile starts separately");
                owner.session.chooseProfile("lime"); owner.changed();
                require(owner.progress().get(Frog.COMMON_FROG).counts[0] == 31, "Returning to the original profile restores its counter");
                owner.save();
                mc.gui.setScreen(null);
            });
        } catch (Exception error) { throw new AssertionError("Collector-to-chat-to-save production checks", error); }
    }
    private static ContainerScreen collector(net.minecraft.client.Minecraft mc, int windowId, long commonBronze) {
        var container = new SimpleContainer(54);
        for (Frog frog : Frog.values()) {
            long[] counts = new long[4];
            if (frog == Frog.COMMON_FROG) { counts[0] = commonBronze; counts[1] = 2; }
            if (frog == Frog.CAVE_FROG) counts[2] = 4;
            if (frog == Frog.PUDDLE_JUMPER) counts[3] = 1;
            container.setItem(10 + frog.ordinal(), item(frog, counts));
        }
        // A matching item in the player's inventory must never be treated as a collector entry.
        mc.player.getInventory().setItem(0, item(Frog.COMMON_FROG, new long[]{999, 999, 999, 999}));
        return new ContainerScreen(ChestMenu.sixRows(windowId, mc.player.getInventory(), container),
            mc.player.getInventory(), Component.literal("Trophy Frogs"));
    }
    private static ItemStack item(Frog frog, long[] counts) {
        var item = new ItemStack(Items.PAPER);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("§a" + frog.label));
        List<Component> lore = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            long amount = counts[tier.ordinal()];
            lore.add(Component.literal(tier.label() + (amount == 0 ? " ✖" : " ✔ (" + amount + ")")));
        }
        item.set(DataComponents.LORE, new ItemLore(lore));
        return item;
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
