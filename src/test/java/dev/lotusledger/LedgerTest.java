package dev.lotusledger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LedgerTest {
    @Test void recordedGameExamplesParse() {
        assertEquals(new GameText.Catch(Frog.COMMON_FROG, Tier.BRONZE),
            GameText.parseCatch("♔ TROPHY FROG! You caught a Common Frog BRONZE!").orElseThrow());
        assertEquals(new GameText.Catch(Frog.PUDDLE_JUMPER, Tier.SILVER),
            GameText.parseCatch("♔ TROPHY FROG! You caught a Puddle Jumper SILVER!").orElseThrow());
    }
    @Test void allVerifiedNamesAndTiersAreAcceptedWithFormatting() {
        assertEquals(12, Frog.values().length);
        for (Frog frog : Frog.values()) for (Tier tier : Tier.values()) {
            // Generated coverage cases, based on the two recorded messages above.
            String line = "§6♔ §r§6§lTROPHY FROG! §fYou caught a §9" + frog.label + " §b§l" + tier + "§f!";
            assertEquals(new GameText.Catch(frog, tier), GameText.parseCatch(line).orElseThrow());
        }
    }
    @Test void unrelatedMessagesCannotCreateCatches() {
        for (String line : new String[]{
            "RIBBIT! [MVP+] _etaF caught their first DIAMOND Common Frog!",
            "Party > Player: ♔ TROPHY FROG! You caught a Common Frog BRONZE!",
            "♔ TROPHY FISH! You caught a Blobfish BRONZE!",
            "♔ TROPHY FROG! You caught a Fictional Frog BRONZE!",
            "♔ TROPHY FROG! You caught a Common Frog MYTHIC!",
            "You picked up Common Frog BRONZE", "", "Common Frog BRONZE"
        }) assertTrue(GameText.parseCatch(line).isEmpty(), line);
    }
    @Test void profileAndHostRecognitionAreStrict() {
        assertEquals("apple", GameText.profile("§aProfile: §bApple ♲").orElseThrow());
        assertEquals("banana", GameText.profile("You are playing on profile: Banana (Co-op)").orElseThrow());
        assertEquals("pear", GameText.profile("Your profile was changed to: Pear").orElseThrow());
        assertTrue(GameText.profile("Party > Player: Profile: Apple").isEmpty());
        assertTrue(GameText.isHypixel("mc.hypixel.net:25565"));
        assertFalse(GameText.isHypixel("hypixel.net.evil.example"));
        assertFalse(GameText.isHypixel("nothypixel.net"));
        assertNotEquals(GameText.profileKey("a", "alpha.hypixel.net", "apple"), GameText.profileKey("a", "mc.hypixel.net", "apple"));
        assertNotEquals(GameText.profileKey("a", "mc.hypixel.net", "apple"), GameText.profileKey("b", "mc.hypixel.net", "apple"));
    }
    @Test void collectionNeverInventsLowerTierCatchesOrErasesObservedOnes() {
        var progress = new LedgerData.Progress();
        assertTrue(progress.hasUnknownHistory());
        assertTrue(progress.markUndiscovered(Frog.COMMON_FROG));
        assertFalse(progress.markUndiscovered(Frog.COMMON_FROG));
        progress.record(new GameText.Catch(Frog.COMMON_FROG, Tier.DIAMOND));
        assertEquals(1, progress.discovered());
        assertTrue(progress.missing(Frog.COMMON_FROG, Tier.BRONZE));
        assertFalse(progress.missing(Frog.COMMON_FROG, Tier.DIAMOND));
        assertFalse(progress.markUndiscovered(Frog.COMMON_FROG));
        progress.record(new GameText.Catch(Frog.CAVE_FROG, Tier.SILVER));
        assertFalse(progress.markUndiscovered(Frog.CAVE_FROG));
        assertFalse(progress.get(Frog.CAVE_FROG).historyKnown);
    }
    @Test void cancelledMessageIsCountedOnceButIdenticalNewPacketsStillCount() {
        var data = new LedgerData();
        var session = new LedgerSession(data, "player", "mc.hypixel.net");
        session.bindWorld(new Object());
        session.chooseProfile("apple");
        var caught = new GameText.Catch(Frog.COMMON_FROG, Tier.BRONZE);
        var packet = new Object();
        assertTrue(session.accept(packet, caught));
        assertFalse(session.accept(packet, caught));
        assertTrue(session.accept(new Object(), caught));
        assertEquals(2, session.progress().get(Frog.COMMON_FROG).counts[0]);
    }
    @Test void bufferedCatchesAndProfileChangesStayIsolated() {
        var data = new LedgerData();
        var session = new LedgerSession(data, "player", "mc.hypixel.net");
        Object world = new Object();
        session.bindWorld(world);
        session.accept(new Object(), new GameText.Catch(Frog.COMMON_FROG, Tier.BRONZE));
        assertNull(session.progress());
        assertEquals(1, session.buffered());
        session.chooseProfile("apple");
        assertEquals(1, session.progress().discovered());
        session.chooseProfile("pear");
        assertEquals(0, session.progress().discovered());
        session.accept(new Object(), new GameText.Catch(Frog.CAVE_FROG, Tier.GOLD));
        session.chooseProfile("apple");
        assertNull(session.progress().get(Frog.CAVE_FROG));
        session.bindWorld(new Object());
        assertNull(session.profile());
        session.accept(new Object(), new GameText.Catch(Frog.SEA_FROG, Tier.GOLD));
        session.bindWorld(new Object());
        session.chooseProfile("pear");
        assertNull(session.progress().get(Frog.SEA_FROG));
    }
    @Test void progressAndSettingsSurviveRestart(@TempDir Path directory) throws Exception {
        Path path = directory.resolve("ledger.json");
        var data = new LedgerData();
        data.settings.x = 123;
        data.settings.scale = 1.4f;
        var progress = new LedgerData.Progress();
        progress.markUndiscovered(Frog.LEAP_FROG);
        progress.record(new GameText.Catch(Frog.LEAP_FROG, Tier.SILVER));
        data.profiles.put("player/main/apple", progress);
        new LedgerStore(path).save(data);
        LedgerData loaded = new LedgerStore(path).load();
        assertEquals(123, loaded.settings.x);
        assertEquals(1.4f, loaded.settings.scale);
        assertEquals(1, loaded.profiles.get("player/main/apple").get(Frog.LEAP_FROG).counts[1]);
        assertTrue(loaded.profiles.get("player/main/apple").get(Frog.LEAP_FROG).historyKnown);
    }
    @Test void damagedSaveRecoversLastGoodBackupAndPreservesDamagedFile(@TempDir Path directory) throws Exception {
        Path path = directory.resolve("ledger.json");
        var store = new LedgerStore(path);
        var data = new LedgerData();
        data.settings.x = 90;
        store.save(data);
        data.settings.x = 180;
        store.save(data);
        Files.writeString(path, "{broken");
        var recoveredStore = new LedgerStore(path);
        var restored = recoveredStore.load();
        assertTrue(recoveredStore.recoveredBackup());
        assertEquals(90, restored.settings.x);
        recoveredStore.save(restored);
        assertEquals(90, new LedgerStore(path).load().settings.x);
        try (var files = Files.list(directory)) { assertTrue(files.anyMatch(file -> file.getFileName().toString().startsWith("ledger.json.corrupt-"))); }
    }
    @Test void malformedDataWithoutBackupIsNotOverwritten(@TempDir Path directory) throws Exception {
        Path path = directory.resolve("ledger.json");
        Files.writeString(path, "{broken");
        var store = new LedgerStore(path);
        assertThrows(java.io.IOException.class, store::load);
        assertThrows(java.io.IOException.class, () -> store.save(new LedgerData()));
        assertEquals("{broken", Files.readString(path));
    }
    @Test void invalidCountsAreRejectedAndScaleIsSanitized() {
        var data = new LedgerData();
        data.settings.scale = Float.NaN;
        data.settings.missingTier = 800;
        data.validate();
        assertEquals(1, data.settings.scale);
        assertEquals(3, data.settings.missingTier);
        var progress = new LedgerData.Progress();
        progress.record(new GameText.Catch(Frog.BULLFROG, Tier.BRONZE));
        progress.get(Frog.BULLFROG).counts[0] = -1;
        data.profiles.put("p/main/apple", progress);
        assertThrows(IllegalArgumentException.class, data::validate);
    }
    @Test void overlayFitsSmallWindowsAndUnknownCountsStayDistinct() {
        var settings = new LedgerData.Settings();
        settings.x = 10000; settings.y = 10000; settings.scale = 2;
        for (int[] size : new int[][]{{320, 240}, {640, 360}, {854, 480}}) {
            var bounds = LedgerHud.bounds(size[0], size[1], settings, HudView.build(settings, null, "Waiting for profile", java.util.Set.of(), false, null).height());
            assertTrue(bounds.x() >= 0 && bounds.y() >= 0);
            assertTrue(bounds.x() + bounds.width() <= size[0]);
            assertTrue(bounds.y() + bounds.height() <= size[1]);
        }
        assertEquals("?", HudView.count(0, false, false));
        assertEquals("0", HudView.count(0, true, false));
        assertEquals("≥3", HudView.count(3, false, true));
        assertEquals("3", HudView.count(3, true, true));
        assertEquals("✔", HudView.count(0, false, true));
    }
}
