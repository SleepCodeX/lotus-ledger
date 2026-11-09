package dev.lotusledger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LedgerData {
    public int schemaVersion = 1;
    public Settings settings = new Settings();
    public Map<String, Progress> profiles = new LinkedHashMap<>();

    public static final class Settings {
        public boolean visible = true;
        public boolean counts = false;
        public int missingTier = -1;
        public int x = 18, y = 60;
        public float scale = 1;

        public void sanitize() {
            scale = Float.isFinite(scale) ? Math.clamp(scale, .6f, 2f) : 1;
            missingTier = Math.clamp(missingTier, -1, 3);
            x = Math.max(0, x);
            y = Math.max(0, y);
        }
    }

    public static final class FrogProgress {
        public long[] counts = new long[4];
        public boolean historyKnown;
        public boolean baselineLoaded;
        public boolean[] collected = new boolean[4];
        public boolean[] exactCounts = new boolean[4];
        public boolean has(Tier tier) { return counts[tier.ordinal()] > 0 || collected[tier.ordinal()]; }
        public boolean exact(Tier tier) { return historyKnown || exactCounts[tier.ordinal()]; }
        public boolean initialized() { return baselineLoaded || historyKnown; }
        public long total() {
            long total = 0;
            for (long count : counts) total = Math.addExact(total, count);
            return total;
        }
    }

    public static final class Progress {
        public Map<Frog, FrogProgress> frogs = new LinkedHashMap<>();
        public FrogProgress get(Frog frog) { return frogs.get(frog); }

        public void record(GameText.Catch caught) {
            FrogProgress entry = frogs.computeIfAbsent(caught.frog(), ignored -> new FrogProgress());
            entry.counts[caught.tier().ordinal()]++;
            entry.collected[caught.tier().ordinal()] = true;
        }

        public boolean markUndiscovered(Frog frog) {
            FrogProgress entry = frogs.computeIfAbsent(frog, ignored -> new FrogProgress());
            if (entry.initialized() || entry.total() > 0) return false;
            entry.historyKnown = true;
            entry.baselineLoaded = true;
            java.util.Arrays.fill(entry.exactCounts, true);
            return true;
        }

        /** Menu values replace local values, including downward corrections. Unknown quantities are never invented. */
        public boolean synchronize(Frog frog, FrogMenu.Entry snapshot) {
            FrogProgress entry = frogs.computeIfAbsent(frog, ignored -> new FrogProgress());
            boolean changed = !entry.baselineLoaded;
            for (Tier tier : Tier.values()) {
                int i = tier.ordinal();
                var value = snapshot.tiers().get(tier);
                if (value == null) throw new IllegalArgumentException("Incomplete menu entry");
                if (value.countKnown() && entry.counts[i] != value.count()) changed = true;
                if (entry.collected[i] != value.collected() || entry.exactCounts[i] != value.countKnown()) changed = true;
                if (value.countKnown()) entry.counts[i] = value.count();
                entry.collected[i] = value.collected();
                entry.exactCounts[i] = value.countKnown();
            }
            entry.baselineLoaded = true;
            entry.historyKnown = snapshot.tiers().values().stream().allMatch(FrogMenu.Value::countKnown);
            return changed;
        }

        public boolean missing(Frog frog, Tier tier) {
            FrogProgress entry = get(frog);
            // A higher tier does not invent a catch of a lower tier.
            return entry == null || !entry.has(tier);
        }

        public int discovered() {
            int total = 0;
            for (FrogProgress entry : frogs.values()) for (Tier tier : Tier.values()) if (entry.has(tier)) total++;
            return total;
        }

        public boolean hasUnknownHistory() {
            for (Frog frog : Frog.values()) if (get(frog) == null || !get(frog).historyKnown) return true;
            return false;
        }
        public int initializedSpecies() {
            return (int) frogs.values().stream().filter(FrogProgress::initialized).count();
        }
        public long totalCatches() {
            long total = 0;
            for (FrogProgress entry : frogs.values()) total += entry.total();
            return total;
        }
    }

    public void validate() {
        if (schemaVersion != 1 || settings == null || profiles == null) throw new IllegalArgumentException("Unsupported ledger data");
        settings.sanitize();
        for (var profile : profiles.entrySet()) {
            Progress progress = profile.getValue();
            if (profile.getKey() == null || progress == null || progress.frogs == null) throw new IllegalArgumentException("Invalid profile");
            for (var frog : progress.frogs.entrySet()) {
                FrogProgress entry = frog.getValue();
                if (frog.getKey() == null || entry == null || entry.counts == null || entry.counts.length != 4
                    || entry.collected == null || entry.collected.length != 4 || entry.exactCounts == null || entry.exactCounts.length != 4)
                    throw new IllegalArgumentException("Invalid frog counts");
                for (long count : entry.counts) if (count < 0 || count > 1_000_000_000L)
                    throw new IllegalArgumentException("Invalid catch count");
            }
        }
    }
}
