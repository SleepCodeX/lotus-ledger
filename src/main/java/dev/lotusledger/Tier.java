package dev.lotusledger;

public enum Tier {
    BRONZE(0xffb98d61), SILVER(0xffc4d1d9), GOLD(0xfff6cd63), DIAMOND(0xff79e3ed);

    public final int color;
    Tier(int color) { this.color = color; }
    public String label() { return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT); }
}
