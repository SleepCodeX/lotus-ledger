# Lotus Ledger

A standalone Trophy Frog collection overlay for **Hypixel SkyBlock's Lotus Atoll**.

**Minecraft 26.2 · Fabric Loader 0.19.5+ · Java 25**

[Download 1.0.0](https://github.com/SleepCodeX/lotus-ledger/releases/tag/1.0.0)

Automatic catches, all **12 species × four tiers**, a compact movable overlay, and saved progress for each player and SkyBlock profile. Checks and count views, a missing-tier filter, scale controls, and five-second catch highlights are included.

![Lotus Ledger's actual Minecraft editor with sample collection data](docs/media/preview.png)

## Install

1. Install Minecraft **26.2**, Java **25**, and Fabric Loader **0.19.5 or newer**.
2. Install **Fabric API 0.161.0+26.2 or newer for Minecraft 26.2**.
3. Put `lotus-ledger-1.0.0.jar` in your instance's `mods` folder.
4. Join Hypixel SkyBlock. Enable the **Profile** and **Area** widgets in `/widget` and enable Player List Info if it is hidden.
5. Visit Lotus Atoll. Run **`/lotusledger`** to move and configure the overlay.

Lotus Ledger can run alongside **SkyHanni** and **Trevor Roll**. Keep their existing jars installed if you use them. This is a separate mod with its own mod ID (`lotusledger`) and its own save file. Fabric Language Kotlin and SkyHanni are not required.

## Use

- **`/lotusledger`** or **`/lotusledger settings`** — settings and layout editor. Drag the card to move it.
- **`/lotusledger toggle`** — show/hide the HUD. Automatic collection continues while it is hidden.
- **`/lotusledger preview`** — sample collection for checking layout. Sample catches are never saved.
- **View: checkmarks/counts** — switch between a collection view and recorded catch totals.
- **Missing Bronze/Silver/Gold/Diamond** — show species without a recorded catch at that exact tier.

The HUD appears on Lotus Atoll. A valid own-catch message also activates it for the current world. It respects the game's hidden-HUD setting. Settings are saved automatically and when the editor closes.

## Existing progress and accuracy

The mod listens to the game's own **TROPHY FROG! You caught a…** system message. Other players' **RIBBIT!** discoveries, chat quotes, item pickups, and sack withdrawals do not create catches.

**Past catches cannot yet be fully imported.** The available verified menu sample only covers `Undiscovered` frogs, and the public API does not expose complete counts per frog and tier. This version never guesses a discovered frog's old totals:

- **`?`** means prior history is unknown. It does not mean the frog was never caught.
- **`✔`** means a catch of that exact tier was recorded by Lotus Ledger.
- In counts view, **`≥3`** means at least three recorded catches; earlier catches may exist.
- Open Ribery's **Trophy Frogs** menu to confirm species still marked **Undiscovered**. Subsequent counts for those species are exact.
- Catching Diamond does not add fictional Bronze, Silver or Gold catches.

Profiles are separated by Minecraft player UUID, Hypixel main/alpha network, and the profile name supplied by Hypixel. If the profile is not available yet, catches are buffered for that world until it is identified. Enable the Profile widget before fishing; unassigned catches cannot safely be carried into a different world/profile.

## Saved data

Progress and layout are stored in **`config/lotus-ledger.json`** inside the Minecraft instance. Writes replace the file atomically where supported, with a last-good `.bak` backup. A damaged file is preserved during backup recovery. If no valid save can be loaded, the mod reports the issue and will not overwrite existing files.

## Build

Use JDK **25**:

```text
./gradlew build
```

The installable jar is `build/libs/lotus-ledger-1.0.0.jar`. The sources jar is supplied separately. This project does not install files into Minecraft or contact Hypixel during its build/tests.

See [VALIDATION.md](VALIDATION.md) for verification and remaining limits, and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the game-data evidence. Original implementation: **MIT license**.
