package dev.lotusledger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CompletedTierTest {
    private static HudView view(LedgerData.Settings settings, LedgerData.Progress progress) {
        return HudView.build(settings, progress, "Lime", Set.of(), false, null);
    }
    private static LedgerData.Progress bronzeCollection() {
        var progress = new LedgerData.Progress();
        for (Frog frog : Frog.values()) progress.record(new GameText.Catch(frog, Tier.BRONZE));
        return progress;
    }
    @Test void defaultShowsAllAndToggleHidesOnlyCompletedTiersWithoutLosingCounts() {
        var settings = new LedgerData.Settings();
        var progress = bronzeCollection();
        var before = view(settings, progress);
        assertEquals(List.of(Tier.values()), before.visibleTiers());
        settings.hideCompletedTiers = true;
        var hidden = view(settings, progress);
        assertEquals(List.of(Tier.SILVER, Tier.GOLD, Tier.DIAMOND), hidden.visibleTiers());
        assertEquals(before.rows(), hidden.rows());
        assertEquals(before.summary(), hidden.summary());
        assertEquals(before.subtitle(), hidden.subtitle());
        assertEquals(12, progress.totalCatches());
        settings.hideCompletedTiers = false;
        assertEquals(before, view(settings, progress));
    }
    @Test void unknownAndPartialCollectionsStayVisibleAndRowFilterCannotCompleteATier() {
        var settings = new LedgerData.Settings();
        settings.hideCompletedTiers = true;
        assertEquals(List.of(Tier.values()), view(settings, null).visibleTiers());
        var progress = bronzeCollection();
        progress.frogs.remove(Frog.COMMON_FROG);
        for (Frog frog : Frog.values()) if (frog != Frog.COMMON_FROG)
            progress.record(new GameText.Catch(frog, Tier.DIAMOND));
        assertEquals(List.of(Tier.values()), view(settings, progress).visibleTiers());
        settings.missingTier = Tier.DIAMOND.ordinal();
        assertEquals(1, view(settings, progress).rows().size());
        assertTrue(view(settings, progress).visibleTiers().contains(Tier.BRONZE));
        progress.record(new GameText.Catch(Frog.COMMON_FROG, Tier.BRONZE));
        assertFalse(view(settings, progress).visibleTiers().contains(Tier.BRONZE));
        assertTrue(view(settings, new LedgerData.Progress()).visibleTiers().contains(Tier.BRONZE));
    }
    @Test void collectedCheckmarksNeedNoNumericCountAndAuthoritativeCorrectionRestoresColumn() {
        var settings = new LedgerData.Settings();
        settings.hideCompletedTiers = true;
        var progress = new LedgerData.Progress();
        var checked = FrogMenu.parse(List.of("Bronze ✔", "Silver ✖", "Gold ✖", "Diamond ✖")).orElseThrow();
        for (Frog frog : Frog.values()) progress.synchronize(frog, checked);
        assertTrue(progress.isTierComplete(Tier.BRONZE));
        assertEquals(0, progress.totalCatches());
        assertFalse(view(settings, progress).visibleTiers().contains(Tier.BRONZE));
        progress.synchronize(Frog.COMMON_FROG, MenuSyncTest.entry(0, 0, 0, 0));
        assertTrue(view(settings, progress).visibleTiers().contains(Tier.BRONZE));
        assertTrue(settings.hideCompletedTiers);
    }
    @Test void allFourCompleteCanHideEveryTierWhileKeepingSpeciesTotals() {
        var settings = new LedgerData.Settings();
        settings.hideCompletedTiers = true;
        settings.counts = true;
        var progress = new LedgerData.Progress();
        for (Frog frog : Frog.values()) progress.synchronize(frog, MenuSyncTest.entry(1, 2, 3, 4));
        var completed = view(settings, progress);
        assertTrue(completed.visibleTiers().isEmpty());
        assertEquals(12, completed.rows().size());
        assertTrue(completed.rows().stream().allMatch(row -> row.total().equals("10")));
        assertEquals("48 / 48 tiers", completed.summary());
        assertEquals("Lime · 120 catches", completed.subtitle());
    }
    @Test void optionPersistsAndExistingConfigDefaultsOff(@TempDir Path directory) throws Exception {
        Path path = directory.resolve("ledger.json");
        Files.writeString(path, "{\"schemaVersion\":1,\"settings\":{\"counts\":true},\"profiles\":{}}");
        var store = new LedgerStore(path);
        var data = store.load();
        assertFalse(data.settings.hideCompletedTiers);
        assertTrue(data.settings.counts);
        data.settings.hideCompletedTiers = true;
        data.profiles.put("lime", bronzeCollection());
        store.save(data);
        var reloaded = store.load();
        assertTrue(reloaded.settings.hideCompletedTiers);
        assertEquals(12, reloaded.profiles.get("lime").totalCatches());
        assertFalse(view(reloaded.settings, reloaded.profiles.get("lime")).visibleTiers().contains(Tier.BRONZE));
    }
}

