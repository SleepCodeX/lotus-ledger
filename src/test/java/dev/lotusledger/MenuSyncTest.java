package dev.lotusledger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MenuSyncTest {
    /** Synthetic quantities using the verified collector grammar; no invented server text format. */
    static FrogMenu.Entry entry(long bronze, long silver, long gold, long diamond) {
        long[] amounts = {bronze, silver, gold, diamond};
        var lines = new java.util.ArrayList<String>();
        for (Tier tier : Tier.values()) lines.add(tier.label() + (amounts[tier.ordinal()] > 0 ? " ✔ (" + amounts[tier.ordinal()] + ")" : " ✖"));
        return FrogMenu.parse(lines).orElseThrow();
    }
    static Map<Frog, FrogMenu.Entry> full(long commonBronze) {
        var map = new EnumMap<Frog, FrogMenu.Entry>(Frog.class);
        for (Frog frog : Frog.values()) map.put(frog, entry(0, 0, 0, 0));
        map.put(Frog.COMMON_FROG, entry(commonBronze, 2, 0, 0));
        return map;
    }
    @Test void completeFormattedCollectorEntryParsesNumbersAndZeros() {
        var parsed = FrogMenu.parse(List.of("§7Your Catches", " §8Bronze §a✔ §7(1,234)", "§7Silver ✔ (5)", "§6Gold ✖", "§bDiamond ✖")).orElseThrow();
        assertEquals(1234, parsed.tiers().get(Tier.BRONZE).count());
        assertTrue(parsed.tiers().get(Tier.BRONZE).countKnown());
        assertEquals(0, parsed.tiers().get(Tier.GOLD).count());
        assertTrue(parsed.tiers().get(Tier.GOLD).countKnown());
        assertFalse(parsed.tiers().get(Tier.GOLD).collected());
    }
    @Test void incompleteMalformedAndConflictingEntriesNeverBecomeZero() {
        for (var lore : List.of(
            List.of("Bronze ✔ (3)", "Silver ✖", "Gold ✖"),
            List.of("Bronze ✔ (3)", "Bronze ✔ (4)", "Silver ✖", "Gold ✖", "Diamond ✖"),
            List.of("Bronze ✖ (3)", "Silver ✖", "Gold ✖", "Diamond ✖"),
            List.of("Bronze ✔ (-3)", "Silver ✖", "Gold ✖", "Diamond ✖"),
            List.of("Undiscovered", "Bronze ✔ (3)"),
            List.of("How to Catch", "This is some unrelated item")
        )) assertTrue(FrogMenu.parse(lore).isEmpty(), lore.toString());
    }
    @Test void collectorCheckWithoutQuantityIsCollectionNotAnInventedCount() {
        var progress = new LedgerData.Progress();
        progress.record(new GameText.Catch(Frog.COMMON_FROG, Tier.BRONZE));
        var parsed = FrogMenu.parse(List.of("Bronze ✔", "Silver ✖", "Gold ✖", "Diamond ✖")).orElseThrow();
        progress.synchronize(Frog.COMMON_FROG, parsed);
        var saved = progress.get(Frog.COMMON_FROG);
        assertEquals(1, saved.counts[0]);
        assertFalse(saved.exact(Tier.BRONZE));
        assertTrue(saved.has(Tier.BRONZE));
        assertTrue(saved.initialized());
        assertEquals("≥1", HudView.count(saved.counts[0], saved.exact(Tier.BRONZE), saved.has(Tier.BRONZE)));
    }
    @Test void eachMenuOpeningReplacesUpOrDownAndRepeatedSnapshotIsIdempotent() {
        var progress = new LedgerData.Progress();
        progress.record(new GameText.Catch(Frog.COMMON_FROG, Tier.BRONZE));
        var sync = new MenuSynchronizer();
        Object first = new Object();
        assertEquals(0, sync.observe(first, progress, 0, full(12)).changedSpecies());
        assertEquals(12, sync.observe(first, progress, 0, full(12)).changedSpecies());
        assertEquals(12, progress.get(Frog.COMMON_FROG).counts[0]);
        assertEquals(12, progress.initializedSpecies());
        assertEquals(0, sync.observe(first, progress, 0, full(12)).changedSpecies());
        sync.close();
        Object second = new Object();
        sync.observe(second, progress, 0, full(3));
        sync.observe(second, progress, 0, full(3));
        assertEquals(3, progress.get(Frog.COMMON_FROG).counts[0], "Authoritative downward correction");
        sync.close();
        Object third = new Object();
        sync.observe(third, progress, 0, full(32));
        sync.observe(third, progress, 0, full(32));
        assertEquals(32, progress.get(Frog.COMMON_FROG).counts[0], "Authoritative upward correction");
    }
    @Test void baselineThenChatThenDisplayedTotalsThenSaveAndReload(@TempDir Path directory) throws Exception {
        var data = new LedgerData();
        var session = new LedgerSession(data, "test-player", "mc.hypixel.net");
        session.bindWorld(new Object());
        session.chooseProfile("lime");
        var sync = new MenuSynchronizer();
        var menu = new Object();
        sync.observe(menu, session.progress(), session.catchRevision(), full(24));
        sync.observe(menu, session.progress(), session.catchRevision(), full(24));
        var packet = new Object();
        var caught = GameText.parseCatch(" TROPHY FROG! You caught Common Frog BRONZE!").orElseThrow();
        session.accept(packet, caught);
        session.accept(packet, caught); // Allow and canceled callbacks for one packet.
        assertEquals(25, session.progress().get(Frog.COMMON_FROG).counts[0]);
        sync.observe(menu, session.progress(), session.catchRevision(), full(24));
        assertEquals(25, session.progress().get(Frog.COMMON_FROG).counts[0], "Unchanged open menu cannot undo a newer catch");
        data.settings.counts = true;
        var view = HudView.build(data.settings, session.progress(), "Lime", Set.of(), false, null);
        var common = view.rows().stream().filter(row -> row.frog() == Frog.COMMON_FROG).findFirst().orElseThrow();
        assertEquals("25", common.cells().get(0));
        assertEquals("27", common.total());
        assertEquals("2 / 48 tiers", view.summary());
        assertEquals("Lime · 27 catches", view.subtitle());
        Path path = directory.resolve("ledger.json");
        new LedgerStore(path).save(data);
        var reloaded = new LedgerStore(path).load();
        assertEquals(25, reloaded.profiles.get("test-player/main/lime").get(Frog.COMMON_FROG).counts[0]);
        session.chooseProfile("pear");
        assertEquals(0, session.progress().totalCatches());
        session.chooseProfile("lime");
        assertEquals(27, session.progress().totalCatches());
    }
    @Test void snapshotObservedBeforeNewCatchIsNotAppliedAfterIt() {
        var progress = new LedgerData.Progress();
        var sync = new MenuSynchronizer();
        Object menu = new Object();
        sync.observe(menu, progress, 0, full(0));
        progress.record(new GameText.Catch(Frog.COMMON_FROG, Tier.BRONZE));
        var blocked = sync.observe(menu, progress, 1, full(0));
        assertEquals(MenuSynchronizer.State.CATCH_DURING_READ, blocked.state());
        assertEquals(1, progress.get(Frog.COMMON_FROG).counts[0]);
        sync.observe(menu, progress, 1, full(2));
        sync.observe(menu, progress, 1, full(2));
        assertEquals(2, progress.get(Frog.COMMON_FROG).counts[0], "A fresh stable server snapshot remains authoritative");
    }
    @Test void currentGameArticleVariantsAndOnboardingAreCovered() {
        assertEquals(Frog.WETLANDS_FROG, GameText.parseCatch(" TROPHY FROG! You caught Wetlands Frog SILVER!").orElseThrow().frog());
        assertEquals(Frog.EXPLODING_FROG, GameText.parseCatch(" TROPHY FROG! You caught an Exploding Frog BRONZE!").orElseThrow().frog());
        assertEquals(Frog.COMMON_FROG, GameText.parseCatch("♔ TROPHY FROG! You caught a Common Frog BRONZE!").orElseThrow().frog());
        var view = HudView.build(new LedgerData.Settings(), new LedgerData.Progress(), "Lime", Set.of(), false, null);
        assertTrue(view.lineOne().contains("Researcher Ribery"));
        assertTrue(view.lineOne().contains("Trophy Frogs"));
        assertEquals("? / 48 tiers", view.summary());
        assertEquals("?", view.rows().getFirst().cells().getFirst());
        assertEquals("?", view.rows().getFirst().total());
    }
    @Test void onlyCompleteEntriesUpdateAndOtherProfilesStayUntouched() {
        var first = new LedgerData.Progress();
        var second = new LedgerData.Progress();
        var sync = new MenuSynchronizer();
        Object menu = new Object();
        var partial = Map.of(Frog.COMMON_FROG, entry(6, 0, 0, 0));
        sync.observe(menu, first, 0, partial);
        sync.observe(menu, first, 0, partial);
        assertEquals(1, first.initializedSpecies());
        assertNull(first.get(Frog.CAVE_FROG));
        sync.observe(menu, second, 0, partial);
        assertEquals(0, second.initializedSpecies(), "Profile switch resets pending snapshot stability");
    }
}
