package dev.lotusledger;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Game grammar verified in SkyblockAPI TrophyFrogAPI, commit 3e570882; see attribution. */
public final class FrogMenu {
    private FrogMenu() {}
    public record Value(long count, boolean countKnown, boolean collected) {}
    public record Entry(Map<Tier, Value> tiers) {
        public Entry {
            if (tiers.size() != 4 || !tiers.keySet().containsAll(java.util.Set.of(Tier.values())))
                throw new IllegalArgumentException("All four tiers are required");
            for (Value value : tiers.values()) if (value == null || value.count < 0 || value.count > 1_000_000_000L)
                throw new IllegalArgumentException("Invalid menu count");
            tiers = Map.copyOf(tiers);
        }
    }

    public static Optional<Entry> parse(List<String> rawLore) {
        Map<Tier, Value> tiers = new EnumMap<>(Tier.class);
        boolean undiscovered = false;
        for (String raw : rawLore) {
            String line = GameText.clean(raw);
            if (line.equals("Undiscovered")) undiscovered = true;
            int space = line.indexOf(' ');
            if (space < 0) continue;
            Tier tier;
            try { tier = Tier.valueOf(line.substring(0, space).toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { continue; }
            String body = line.substring(space + 1).strip();
            int quantityAt = body.indexOf(" (");
            String marker = quantityAt < 0 ? body : body.substring(0, quantityAt);
            boolean yes = marker.equals("✔") || marker.equals("✓");
            boolean no = marker.equals("✖");
            if (!yes && !no) return Optional.empty();
            Value value;
            if (quantityAt >= 0) {
                if (!body.endsWith(")")) return Optional.empty();
                String quantity = body.substring(quantityAt + 2, body.length() - 1);
                if (!quantity.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)")) return Optional.empty();
                long count;
                try { count = Long.parseLong(quantity.replace(",", "")); }
                catch (NumberFormatException ignored) { return Optional.empty(); }
                if (count > 1_000_000_000L || (no && count != 0)) return Optional.empty();
                value = new Value(count, true, yes);
            } else {
                // A check without a quantity confirms collection, not an invented numeric total.
                value = new Value(0, no, yes);
            }
            if (tiers.putIfAbsent(tier, value) != null) return Optional.empty();
        }
        if (undiscovered) {
            if (!tiers.isEmpty()) return Optional.empty();
            for (Tier tier : Tier.values()) tiers.put(tier, new Value(0, true, false));
        }
        return tiers.size() == 4 ? Optional.of(new Entry(tiers)) : Optional.empty();
    }
}
