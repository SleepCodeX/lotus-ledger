package dev.lotusledger;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Immutable display text, rebuilt only when data/options change, never once per rendered frame. */
public record HudView(List<Row> rows, String summary, String subtitle, String lineOne, String lineTwo, String emptyLine) {
    public record Row(Frog frog, String total, List<String> cells, List<Boolean> collected, boolean highlighted) {}
    public HudView { rows = List.copyOf(rows); }
    public int height() { return 64 + Math.max(1, rows.size()) * 13 + 35; }
    public static HudView build(LedgerData.Settings settings, LedgerData.Progress progress, String profile,
                               Set<Frog> highlights, boolean preview, MenuSynchronizer.Result sync) {
        List<Row> rows = new ArrayList<>(12);
        for (Frog frog : Frog.values()) {
            if (settings.missingTier >= 0 && progress != null && !progress.missing(frog, Tier.values()[settings.missingTier])) continue;
            var entry = progress == null ? null : progress.get(frog);
            List<String> cells = new ArrayList<>(4);
            List<Boolean> collected = new ArrayList<>(4);
            for (Tier tier : Tier.values()) {
                boolean has = entry != null && entry.has(tier);
                boolean exact = entry != null && entry.exact(tier);
                long count = entry == null ? 0 : entry.counts[tier.ordinal()];
                cells.add(settings.counts ? count(count, exact, has) : has ? "✔" : exact ? "–" : "?");
                collected.add(has);
            }
            rows.add(new Row(frog, total(entry == null ? 0 : entry.total(), entry != null && entry.historyKnown),
                List.copyOf(cells), List.copyOf(collected), highlights.contains(frog)));
        }
        int tiers = progress == null ? 0 : progress.discovered();
        int initialized = progress == null ? 0 : progress.initializedSpecies();
        boolean allCounts = progress != null && !progress.hasUnknownHistory();
        long catches = progress == null ? 0 : progress.totalCatches();
        String amount = allCounts ? NumberFormat.getIntegerInstance(Locale.ROOT).format(catches)
            : catches > 0 ? "≥" + NumberFormat.getIntegerInstance(Locale.ROOT).format(catches) : "?";
        String subtitle = preview ? "Preview · sample catches stay separate" : profile + " · " + amount + " catches";
        String first, second;
        if (preview) {
            first = "Drag the card to find your spot.";
            second = "Your real collection stays unchanged.";
        } else if (profile.equals("Waiting for profile")) {
            first = "Enable Profile in /widget.";
            second = "Then open Ribery's Trophy Frogs menu.";
        } else if (sync != null && sync.state() == MenuSynchronizer.State.CATCH_DURING_READ) {
            first = "New catch saved while the menu was updating.";
            second = "Reopen Trophy Frogs to finish the sync.";
        } else if (sync != null && sync.state() == MenuSynchronizer.State.READING) {
            first = "Reading Researcher Ribery's menu…";
            second = sync.parsedSpecies() + "/12 complete entries found.";
        } else if (initialized < 12) {
            first = "Visit Researcher Ribery → Trophy Frogs.";
            second = initialized + "/12 species synced · ? means not loaded yet.";
        } else {
            first = "Synced with Ribery · catches update live.";
            second = allCounts ? "Open Trophy Frogs any time to refresh." : "✔ collected · ≥ recorded · ? count unavailable";
        }
        String empty = settings.missingTier < 0 ? "Your collection will appear here." : "All " + Tier.values()[settings.missingTier].label() + " tiers collected!";
        String tierSummary = initialized == 12 ? Integer.toString(tiers) : tiers > 0 ? "≥" + tiers : "?";
        return new HudView(rows, tierSummary + " / 48 tiers", subtitle, first, second, empty);
    }
    static String count(long value, boolean exact, boolean collected) {
        if (!exact && value == 0) return collected ? "✔" : "?";
        return (exact ? "" : "≥") + compact(value);
    }
    static String total(long value, boolean exact) { return !exact && value == 0 ? "?" : (exact ? "" : "≥") + compact(value); }
    private static String compact(long value) {
        if (value < 1_000) return Long.toString(value);
        long divisor = value < 1_000_000 ? 1_000 : value < 1_000_000_000 ? 1_000_000 : 1_000_000_000;
        String suffix = divisor == 1_000 ? "k" : divisor == 1_000_000 ? "m" : "b";
        return String.format(Locale.ROOT, "%.1f", value / (double) divisor).replace(".0", "") + suffix;
    }
}
