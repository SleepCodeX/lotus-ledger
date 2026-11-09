# Lotus Ledger 1.1.0

A standalone, automatic Trophy Frog tracker for Hypixel SkyBlock's Lotus Atoll.

- Synchronizes existing frogs, tiers and numeric counts from Researcher Ribery's **Trophy Frogs** menu every time it opens.
- Treats complete recognized menu values as authoritative, correcting counters upward or downward.
- Immediately updates and saves progress from incoming trophy frog chat messages.
- Separates all 12 species and four tiers, per player and SkyBlock profile.
- Prevents duplicate counting, additive menu imports, and stale menu snapshots undoing newer catches.
- Adds rounded, readable typography, a calm compact overlay, movable layout, checkmark/count views and missing-tier filters.
- Shows clear Ribery setup guidance instead of turning unknown progress into zero.
- Reuses prepared HUD text and cached menu results.

**Install:** Minecraft 26.2, Java 25, Fabric Loader 0.19.5+ and Fabric API 0.161.0+26.2 or newer for Minecraft 26.2. Put **lotus-ledger-1.1.0.jar** in **mods**, removing older Lotus Ledger jars.

**Keep SkyHanni installed. Lotus Ledger is standalone and runs alongside it.**

Open **/lotusledger** for settings. Enable **Profile** and **Area** in **/widget**, then talk to **Researcher Ribery** and open **Trophy Frogs** to load your baseline. New catches update automatically. Reopen the menu whenever you want the authoritative values refreshed.

**Verification:** 20 logic/menu/persistence checks plus actual packaged-client checks of menu ItemLore import, chat increment, displayed totals, immediate save/reload, upward/downward reconciliation, unchanged reopening, profile separation, font loading and layout controls.

**Limit:** validation used an isolated offline Minecraft client with controlled collector items and recorded game chat formats. A live Hypixel session and the user's full modpack were not exercised. Unknown or incomplete tooltip values remain unknown rather than becoming fabricated zeros.

Sources, full buildable source archive, checksums, MIT license and font attribution are supplied with this release.
