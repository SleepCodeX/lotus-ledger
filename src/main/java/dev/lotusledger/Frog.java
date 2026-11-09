package dev.lotusledger;

import java.util.Optional;

/** Game names/IDs verified against NEU item data, f315a139 (2026-11-09). */
public enum Frog {
    COMMON_FROG("Common Frog"),
    EXPLODING_FROG("Exploding Frog"),
    LEAP_FROG("Leap Frog"),
    WETLANDS_FROG("Wetlands Frog"),
    REALITY_HOPPER("Reality Hopper"),
    BLESSED_FROG("Blessed Frog"),
    BULLFROG("Bullfrog"),
    SEA_FROG("Sea Frog"),
    CAVE_FROG("Cave Frog"),
    HIGHLANDS_FROG("Highlands Frog"),
    TREE_FROG("Tree Frog"),
    PUDDLE_JUMPER("Puddle Jumper");

    public final String label;
    Frog(String label) { this.label = label; }
    public static Optional<Frog> fromName(String name) {
        for (Frog frog : values()) if (frog.label.equals(name)) return Optional.of(frog);
        return Optional.empty();
    }
}
