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
- Milestone 4 (`DebugQuest`) and 5 (menu, art, deck) are not started.
