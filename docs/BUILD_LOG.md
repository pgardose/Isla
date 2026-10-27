# Build Log: Milestones 1-3

Branch `feature/milestones-1-3`, 11 commits, built test-first (each task: failing tests, watch them fail, implement, watch them pass).
Size: ~2764 lines of main code, ~1523 lines of tests, 112 tests.

## What was built
| Milestone | Delivered |
|---|---|
| 1 Walkable island | Maven + JavaFX shell, 40x26 tile map from a text file, colored-square sprites, camera, movement with collision, locked jungle, stamina drain |
| 2 Interaction and UI | Vendor shops with stock quantities, dialogue box, buy flow (money / inventory-full / sold-out rules), inventory with category tabs, item use, rest, HUD, Elder's fetch quest |
| 3 Persistence | MySQL schema, 4 DAO interfaces with in-memory and JDBC implementations, save/continue (F5 + autosave on close), no-database fallback |

## Verification evidence
- 112 JUnit tests pass. The 19 database tests (13 DAO contract + 6 save/continue) run only when `-Disla.test.jdbcUrl` is set; they were run and passed against a live MariaDB 10.11 server (MySQL-compatible SQL, MariaDB driver). With no database they are skipped, the other 93 pass.
- The JavaFX renderer was run under a virtual display and screenshots were checked by eye (`docs/screenshots`). Two visual bugs found that way (HUD showing through panels, inventory highlight overlapping the tabs) were fixed.
- Tests include two full play-throughs: buy a coconut, walk to the elder, get paid; and buy the machete, use it, enter the jungle.

## Not verified here (please check on your machine)
- **`pom.xml` was not run.** The sandbox cannot reach Maven Central, so I compiled with `javac` against the JavaFX 11 and JUnit 5.9 packages from Ubuntu instead. The pom targets JavaFX 21 / Java 21 / Connector/J 8.4 with standard plugin settings; if `mvn javafx:run` complains, send me the error.
- **The real window and keyboard loop** (AnimationTimer, key repeat, close-to-autosave) is compile-checked only. Rendering was verified, live input was not.
- **Real MySQL.** Tests ran on MariaDB. The SQL is plain InnoDB DDL and `ON DUPLICATE KEY UPDATE`, which MySQL 8 supports, but I have not run it on MySQL itself.
- The "database unreachable, fall back to memory" path in `IslaApp` is not unit tested.

## Decisions made while building (rulings)
1. **Stamina has a gameplay effect:** 1 stamina per 10 steps; at 0 you cannot walk until you rest (R, +10) or use a consumable. The spec did not say, and a stamina bar that never changes made consumables pointless.
2. **`use()` returns a message instead of printing**, and `interact()` returns an `InteractionResult`, so the UI can show them. The class diagram changed accordingly (see README).
3. **Consumables are not wasted:** using one at full stamina is refused ("Stamina is already full.") and the item is kept (`Item.getUnusableReason`).
4. **Shop stock has quantities** (spec section 6 flagged this); `Shop` is now `Map<Item,Integer>`.
5. **Extra schema:** table `unlocked_areas` and columns `items.unlocks_area`, `quests.required_item`, `vendors.sprite`, `vendors.dialogue`, because the original columns could not hold tool areas, fetch-quest requirements or vendor appearance.
6. **World content lives in Java (`SeedData`)** and seeds the database on first run, so there is one source of truth instead of a separate seed.sql.
7. **One shared `DaoContractTest`** instead of four contract classes.
8. **Unlocked areas are persisted**, and the player is never given the old console `Main`; the JavaFX app replaces it.
9. Player name defaults to "Traveler" (there is no name entry yet).

## Known limitations / next steps
- A shop panel draws at most 6 items cleanly (current stock is 3 per vendor).
- No main menu yet: the game loads your save automatically; `--new` starts over.
- Milestone 5 was not started at this point (see the Milestone 4 and 5a sections below).

---

# Milestone 4: DebugQuest

Built test-first like 1-3 (each step: failing test, watch it fail, minimal code, watch it pass). Tests went from 112 to 152.

## What was built
| Piece | Delivered |
|---|---|
| Model | `DebugQuest extends Quest` (snippet, options, correct index, `answer()`); `Quest.interact()` hook that `DebugQuest` overrides; `InteractionResult.Kind.PUZZLE` |
| UI flow | `Screen.DEBUG_PUZZLE` in `GameSession` (Up/Down wraps, Z submits, X leaves, wrong answer = message and retry), puzzle panel in `renderPuzzle` |
| World | New NPC **Coder Nia** at (24,14) with a $40 puzzle (`if (stamina = 0)`, correct fix is option B); teal sprite |
| Persistence | Tables `debug_quests` and `quest_options`; in-memory and JDBC DAOs return the right type; completion saved in `quests.is_completed` |
| Bootstrap | Each quest is seeded on its own, so a save made before Milestone 4 gains Nia on the next start |

## Verification evidence
- 152 tests pass with the 28 database tests included, run against a live MariaDB 10.11 server (the 9 new ones cover puzzle round-trip, mixed quest types, re-save without stale options, delete, and save/continue). Without a database the 124 non-database tests pass and 28 are skipped.
- The new screen was rendered through the real JavaFX renderer under a virtual display and checked by eye (`docs/screenshots/shot_puzzle.png`). That check found that Nia had no sprite colour (magenta fallback), now fixed.

## Not verified here (please check on your machine)
- Same as before: `pom.xml`/`mvn javafx:run` was not run, live keyboard input was not exercised, and the SQL ran on MariaDB rather than MySQL 8.
- The new tables use plain InnoDB DDL with `ON DELETE CASCADE`. The sprite colour entry has no unit test (`JavaFxRenderer` needs a JavaFX runtime).

## Decisions made while building
1. **Wrong answers are free** and can be retried; the spec only required a reward on success. Easy to change in `DebugQuest.answer`.
2. **Separate tables instead of new columns on `quests`**, because `CREATE TABLE IF NOT EXISTS` would never add columns to a database that already exists.
3. **The correct option is B, not A**, so the first choice is not always right.
4. **Quest type decides behaviour** through `Quest.interact()` (polymorphism) instead of `instanceof` checks in `QuestGiver`.

---

# Milestone 5a: Main menu

Only the main menu part of Milestone 5 was built. Real art, the Figma prototype and the pitch deck are **not** done. Built test-first; tests went from 152 to 200.

## What was built
| Piece | Delivered |
|---|---|
| `AppFlow` (new, `isla.game`) | The layer above `GameSession`: modes MENU, SETTINGS, CONFIRM_NEW, CONFIRM_ERASE, PLAYING. No JavaFX imports, so it is unit tested |
| Main menu | Continue (greyed out with no save), New Game, Settings, Quit; cursor wraps; starts on Continue when a save exists |
| Safety questions | New Game over a save and Erase saved game both ask first; the default answer is No |
| `Settings` (new) | Autosave on close (On/Off), stored in `settings.properties`; missing or corrupt file falls back to defaults |
| Leaving the island | X/Esc while walking returns to the menu (saves first if autosave is on); inside a shop, dialogue or puzzle it only closes that |
| `GameBootstrap` | New `hasSave()` and `erase()` |
| `IslaApp` | Talks to `AppFlow`; held-key repeat only while walking; Quit exits; window close autosaves; `--new` still skips the menu |

## Verification evidence
- 200 tests pass with the database tests included, run against a live MariaDB 10.11 server. Without a database 169 pass and 31 are skipped.
- Menu, overwrite question and settings screens rendered through the real JavaFX renderer under a virtual display and checked by eye (`docs/screenshots/shot_menu.png`, `shot_settings.png`, `shot_confirm.png`).

## Not verified here (please check on your machine)
- Same as before: `pom.xml` / `mvn javafx:run` was not run, and live keyboard input was not exercised. `IslaApp` is compile-checked and screenshot-checked only.
- Specifically worth trying by hand: hold an arrow key in the menu (it should move one step per press), Quit, closing the window mid-game, and `--new`.
- The path where saving fails while leaving the island (the game stays on the island and shows "Could not save") has no unit test.

## Decisions made while building
1. **"Start" became Continue + New Game**, because Start is ambiguous once a save exists.
2. **Settings contents were my choice** (autosave toggle, erase save); the spec did not define them.
3. **X/Esc while walking now leaves to the menu.** With autosave off, progress since the last save is not kept (the menu says so).
4. **The cursor returns to its default (Continue if a save exists, otherwise New Game)** whenever you come back to the menu.
5. **Autosave on close now also runs with in-memory storage** (harmless; it just lets Continue work within one run).

---

# Built-in database (H2)

Saving used to need a MySQL server; without one the game ran from memory and could not save. It now has a built-in database that needs no install. Built test-first; tests went from 200 to 210.

## What was built
| Piece | Delivered |
|---|---|
| `Database.embedded(folder)` / `shutdown()` | H2 in file mode with `MODE=MySQL`, stored in `isla-data/` next to `pom.xml`; kept open for the whole run and closed cleanly on exit |
| `StorageOpener` + `Storage` (new, `isla.dao`) | Picks the storage: MySQL (if `db.properties` exists and answers), else the built-in database (with a notice if MySQL was expected), else memory only (with "progress will not be saved"). Never throws |
| `IslaApp` | Uses `StorageOpener`; `stop()` closes the database. The old inline MySQL-or-memory logic is gone |
| `pom.xml` | `com.h2database:h2:2.2.220`, runtime scope |
| Tests | The 31 database tests (DAO contract + save/continue) now run by default on an in-memory H2, so they are no longer skipped; setting `-Disla.test.jdbcUrl` still points them at MySQL |
| Docs | README "Saving: works out of the box", MySQL is optional; `isla-data/` and `settings.properties` are git-ignored |

## Verification evidence
- 210 tests pass, 0 skipped, on H2 (the default) **and** again against a live MariaDB 10.11 server (`-Disla.test.jdbcUrl`). The existing MySQL SQL (`ON DUPLICATE KEY UPDATE`, `ENGINE=InnoDB`, foreign keys with cascade) ran on H2 unchanged.
- A real persistence test saves, shuts the database down, reopens it from the files and gets the progress back.
- The real `IslaApp` was launched under a virtual display in an empty folder: it ran without errors and created `isla-data/isla.mv.db` by itself.

## Not verified here (please check on your machine)
- Same as before: `mvn javafx:run` was not run (no Maven Central here). I tested with the Ubuntu copy of H2 2.2.220, the same version as the `pom.xml` entry, but the Maven download itself is untried.
- A real two-launch check by hand: play, press F5, close, reopen, Continue. Also try opening two Isla windows at once: the second should show "No database available".
- Not tested on Windows or macOS (the data folder path is built with `Path`, so it should work).

## Decisions made while building
1. **H2, not SQLite**, because its MySQL mode runs the existing SQL and DAOs unchanged (SQLite would have needed different upsert syntax).
2. **Data folder `isla-data/` in the working directory**, next to `db.properties`.
3. **MySQL down falls back to the built-in database** (with a notice) instead of memory. Saves are not copied between the two.
4. **One Isla window at a time** can use the built-in database (H2 locks the file); a second one falls back to memory with a notice.
