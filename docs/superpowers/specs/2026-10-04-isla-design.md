# Isla: Design Spec

Approved by Pj on 2026-10-04. Milestones 1-3 are implemented from this spec; milestones 4-5 are not.

## 1. Goal
Turn the existing console-level OOP core into a playable JavaFX island game that meets the course requirements:
- Encapsulation, inheritance, polymorphism and abstraction, demonstrated in code.
- Class and sequence diagrams.
- SQL via JDBC/DAO.
- The teacher's debug-the-code puzzle (milestone 4).
- Material for the Figma prototype and pitch deck.

## 2. Decisions
| Area | Decision |
|---|---|
| Rendering | JavaFX (`Canvas` + `AnimationTimer`) |
| Art | Colored-square placeholders, swappable later |
| Database | MySQL via JDBC behind DAO interfaces |
| Build | Maven (JavaFX and the MySQL driver as dependencies) |
| Tests | JUnit 5 for game logic |
| Scope | Single player, one island, per the README |

## 3. Architecture

**Packages**
- `isla`: existing model classes, kept in place.
- `isla.world`: `TileType`, `TileMap`.
- `isla.ui`: JavaFX app, screens, input, the `Renderer` implementation.
- `isla.dao`: DAO interfaces plus in-memory and MySQL implementations.

**Model/UI split.** `Entity.render()` becomes `render(Renderer r)`. `Renderer` is a small interface (`drawTile`, `drawSprite`, `drawText`) in the model layer. A JavaFX class implements it with a `GraphicsContext`. The OOP hierarchy never imports JavaFX, and `render` stays polymorphic across `Player`, `Vendor` and `QuestGiver`.

**World**
- `TileType` is an enum: GRASS, SAND, WATER, PATH, HUT, and so on. Each has a color and a `walkable` flag.
- `TileMap` loads from a plain text file, one character per tile.
- `Player.move(dx, dy)` checks `TileMap` for walkability and for `unlockedAreas` (the Tool mechanic).

**Game loop and input.** `AnimationTimer` calls `update()` then render each frame. An `InputState` class tracks held keys. Movement is tile by tile (GBA style) with a short cooldown.

**Screens.** A small state switch: EXPLORING, SHOP, INVENTORY, DEBUG_PUZZLE, MAIN_MENU. Each state owns its input handling and drawing. The HUD (money, stamina) is drawn over the exploring view.

**Persistence**
- DAO interfaces: `PlayerDAO`, `ItemDAO`, `InventoryDAO`, `VendorDAO`, `QuestDAO`, sharing a generic `Repository<T>` (`save`, `findById`, `delete`).
- Two implementations each: in-memory (default, used by tests and development) and MySQL (JDBC).
- The schema matches the README: `players`, `items`, `inventory`, `vendors`, `shop_stock`, `quests`.
- Connection settings come from a `db.properties` file.
- The game runs with no MySQL installed.

## 4. Milestones
1. **Walkable island.** Maven project, JavaFX window, game loop, tile map, player movement with collision.
2. **Interaction and UI.** `Interactable` triggered by the action key near an NPC, shop screen, purchase flow, inventory screen with category tabs, HUD, simple fetch-quest handling.
3. **Persistence.** MySQL schema script, JDBC connection, DAO implementations, save and continue.
4. **DebugQuest.** `DebugQuest extends Quest`; multiple-choice fix of a broken snippet; correct answer pays the reward. *(not in this delivery)*
5. **Polish and docs.** Main menu, updated UML and sequence diagrams, deck material. *(not in this delivery)*

## 5. Testing
JUnit tests cover the logic: shop purchases, inventory capacity, movement and collision, quest completion, DAOs against the in-memory implementation. The JavaFX window and the MySQL integration are verified locally.

## 6. Known issues in the current code (at spec time)
- `Shop.buyItem` removes the item after one sale, so every vendor item was single-stock. Changed in milestone 2 to support quantities.
- `Main`'s demo comments were out of sync with its behaviour. `Main` is replaced by the JavaFX entry point.

## 7. DebugQuest (milestone 4)
`DebugQuest extends Quest` with a broken snippet, fix options and the index of the correct one. A `QuestGiver` holding it opens the DEBUG_PUZZLE screen; a correct answer completes the quest and pays the reward; a wrong answer allows a retry. v1 is multiple choice.

## 8. Out of scope
Multiplayer, combat, procedural maps, mobile, real art assets.
