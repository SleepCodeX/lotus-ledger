# Lotus Ledger 1.2.0

Adds **Hide completed tiers** in **/lotusledger**.

- Turn it on to hide Bronze once all 12 Bronze frogs are collected. Silver, Gold and Diamond follow the same rule independently.
- Starts off, saves between sessions, and uses the entire current profile even with a missing-tier row filter active.
- Hidden tiers keep contributing to all catch totals and collection progress. Turning the option off restores every column.
- Unknown or incomplete tiers stay visible. Collector-menu corrections automatically restore a tier if it becomes incomplete.
- Remaining columns spread out for readability. Completing all four shows **All tiers collected** with species totals.
- Settings controls also fit the compact 320 × 240 GUI viewport.
- Makes the card background much more transparent (44% opacity), softens its shadow and lightens the settings backdrop. Text keeps its existing colors and opacity.

**Install:** Minecraft 26.2, Java 25, Fabric Loader 0.19.5+ and Fabric API 0.161.0+26.2 or newer for Minecraft 26.2. Replace your older Lotus Ledger jar with **lotus-ledger-1.2.0.jar** in **mods**.

**Keep SkyHanni installed. Lotus Ledger runs alongside it.** Existing collection data and settings are retained.

**Verification:** 25 logic/menu/persistence tests and actual packaged Minecraft client checks, including clicking the new option, count preservation, saved settings, compact layout, collector corrections and the all-complete view.

Validation uses an isolated offline Minecraft client with controlled collector items and recorded game-message formats. Live Hypixel and the user's full modpack were not exercised.

Buildable source, checksums, MIT license and font attribution are included.

