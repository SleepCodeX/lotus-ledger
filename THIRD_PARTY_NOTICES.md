# Sources and attribution

Lotus Ledger is an independent Java/Fabric implementation under the MIT license. It does not contain SkyHanni code or bundle SkyHanni, Feesh, or Trevor Roll. It does not modify or depend on any of those mods.

Game facts were verified on 2026-11-09:

- The 12 frog names and their four item tiers were checked against [NotEnoughUpdates-REPO](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/tree/f315a139f067c1ac65272b70b294336b5327038e/items), commit `f315a139f067c1ac65272b70b294336b5327038e`. This repository's data is [MIT licensed](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/blob/f315a139f067c1ac65272b70b294336b5327038e/LICENSE), copyright (c) 2020 Moulberry. No item textures or item JSON files are bundled.
- Two captured game-message samples are documented in [Feesh's game research notes](https://github.com/Sleepy-Panda/Feesh/blob/a7bf140bbccfb10180faebab52062c9950f5b732/docs/dev/Future%20ideas%20%26%20requests.md). Only the game's message text was used as evidence; no Feesh implementation was copied.
- The `Trophy Frogs` menu name and `Undiscovered` tooltip state were verified in the [Researcher Ribery interface](https://hypixelskyblock.minecraft.wiki/w/Researcher_Ribery#Interface). Discovered tier-and-count grammar was verified against [SkyblockAPI's maintained frog menu parser](https://github.com/SkyblockAPI/SkyblockAPI/blob/3e570882de8b552e81d535f3de6b0b5d356288a2/src/main/kotlin/tech/thatgravyboat/skyblockapi/api/area/atoll/trophyfrog/TrophyFrogAPI.kt). Only game-format facts were used; the Java parser and synchronization implementation are original.
- Profile announcement text and tab widget labels were checked against [SkyHanni's game parsers](https://github.com/SkyHanniStudios/SkyHanni/tree/960181ecf67e9c94c94169bdd39d9f7f3ff3192c/src/main/java/at/hannibal2/skyhanni/data). SkyHanni was examined for compatibility and game facts; the abandoned local fork is not part of this standalone project.

Fabric Loader, Fabric API, Minecraft and Gradle are provided through their own distributions and retain their respective licenses. The standard Gradle wrapper is redistributed unmodified.

The rounded typeface is a static, Latin subset derived at weight 600 from [Nunito](https://github.com/googlefonts/nunito/tree/8c6a9bb9732545b9ed53f29ec5e1ab0ff53c4e6f), copyright 2014 The Nunito Project Authors. The modified asset is named **Lotus Rounded**, retains the SIL Open Font License, and includes that license at `assets/lotusledger/NUNITO-OFL.txt`. The reproducible preparation script is in `tools/prepare_font.py`.

The current no-article and `an Exploding Frog` chat variants were also confirmed from local Minecraft game-message captures on 2026-11-09. Test fixtures contain only game text and synthetic quantities, without player identifiers or local paths.
