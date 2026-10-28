# Isla: A 2D Island Trading Adventure

Java OOP course project (UIC, College of Computing Education): a GBA-style top-down island game.
Walk around a tile-based island, buy items from vendors on a limited budget, manage a capacity-limited
inventory, and run a simple fetch quest. Progress is saved to MySQL.

**Author:** Pearl Kristian M. Gardose (Pj)

**Status:** milestones 1-4 done (walkable island, interaction/shop/inventory UI, MySQL persistence, `DebugQuest` puzzle)
plus the **main menu** from milestone 5 (Continue / New Game / Settings / Quit). Still open from milestone 5:
real art, the Figma prototype and the pitch deck.

![Exploring](docs/screenshots/shot_explore.png)
![Shop](docs/screenshots/shot_shop.png)
![Debug puzzle](docs/screenshots/shot_puzzle.png)
![Main menu](docs/screenshots/shot_menu.png)
![Settings](docs/screenshots/shot_settings.png)

## Run it

Requirements: JDK 21, Maven 3.9+. Nothing else: saving uses a built-in database (H2) that Maven downloads for you.
MySQL is optional.

```bash
mvn javafx:run          # play
mvn test                # unit tests (the database tests run on the built-in H2; MySQL is optional, see below)
```

The game opens on the main menu. Add `--new` to skip the menu and start over, discarding the saved game:
`mvn javafx:run -Djavafx.args="--new"`.

### Saving: works out of the box

Progress is saved with **F5**, automatically when you leave the island with X/Esc, and when the window closes
(unless you turn **Autosave on close** off in Settings). The game stores it in a **built-in database** in the
`isla-data/` folder next to `pom.xml` (created on first run, ignored by git). Delete that folder, or use
*Settings > Erase saved game*, to start from scratch.

The game picks where to save like this:

1. **MySQL**, if a `db.properties` file exists and the server answers (see below).
2. Otherwise the **built-in database**. If `db.properties` exists but MySQL is down, you get a notice and the built-in one is used.
3. Only if the built-in database cannot open either (read-only folder, or a second Isla window already has it open)
   does the game run from memory and show "No database available - progress will not be saved."

### Optional: use your own MySQL server

```sql
CREATE DATABASE isla CHARACTER SET utf8mb4;
```

Copy `db.properties.example` to `db.properties` (next to `pom.xml`) and fill in the URL, user and password.
The game creates the tables and seeds the island on first run (`src/main/resources/db/schema.sql`).
Saves already in the built-in database are not copied over to MySQL.

The DAO and save/continue tests run on a throwaway in-memory H2 database by default. To run them on MySQL instead
(they **empty** the tables in the database you name, so use a throwaway one):

```bash
mvn test -Disla.test.jdbcUrl=jdbc:mysql://localhost:3306/isla_test \
         -Disla.test.jdbcUser=isla -Disla.test.jdbcPassword=isla
```

## Controls

| Key | Action |
|---|---|
| Arrows / WASD | Walk, move cursor (Up/Down chooses a puzzle option) |
| Z / Enter / Space | Talk, confirm, buy, use, submit a puzzle answer |
| X / Esc / Backspace | Cancel, leave |
| I | Inventory (Left/Right switches tabs) |
| R | Rest (+10 stamina) |
| F5 | Save |
| X / Esc (while walking) | Leave the island back to the main menu (saves first if autosave is on) |

## How the game works

- **Main menu:** *Continue* loads your save (greyed out when there is none), *New Game* starts over (and asks
  "Overwrite your saved game?" first, defaulting to No), *How to Play* (goal and controls), *Settings*, *Quit*.
- **Settings:** *Autosave on close* On/Off (remembered in `settings.properties`) and *Erase saved game* (asks first).

- Start with $50, 100 stamina, 6 inventory slots. Walking costs 1 stamina per 10 steps; at 0 you must rest (R) or use a consumable.
- Vendors (`!`) sell items with limited stock. Buying is blocked without enough money or with a full inventory (you are never charged for nothing).
- **Elder Kai** (`?`) wants a Coconut (sold by Mara for $8) and pays $25 once.
- **Coder Nia** (teal square, below the main road near the start) has a broken line of code. Pick the fix from four options. A wrong pick costs nothing and you can retry; the right one pays $40 once.
  Every **new game draws a different puzzle** from a bank of 13 (never the one you had last time) with the answers shuffled; **Continue keeps your puzzle**.
  Add your own in `DebugQuestBank.java` (one entry each).
- The world is now **80x52**: the home island, a village island to the east (follow the road across the bridge) and a big island to the south (the bridge at the bottom of the home island).
- The **Rusty Machete** (Tomas, $30) is a Tool: use it from the inventory to unlock the **Jungle Path**, which hides Yara's rare items.

## Architecture

```
isla            model: Entity, Player, NPC, Vendor, QuestGiver, Item (+3 subclasses), Inventory, Shop, Quest,
                       Interactable, InteractionResult, Renderer (interface)
isla.world      TileType, TileMap (loads maps/island.txt), GameWorld (movement, collision, interaction)
isla.game       AppFlow (menu / settings / playing), Settings, GameSession (Actions -> state -> drawing),
                GameBootstrap (start/save/erase), SeedData (starting content)
isla.dao        Repository<T> + ItemDAO/PlayerDAO/VendorDAO/QuestDAO, in-memory and JDBC implementations,
                Database (MySQL or built-in H2), StorageOpener (picks which one to use)
isla.ui         JavaFX only: IslaApp, JavaFxRenderer, KeyBindings
```

Only `isla.ui` imports JavaFX. Everything else draws through the small `Renderer` interface, so the whole game
is unit-tested without a window (243 tests).

### OOP pillars

- **Encapsulation:** `Player.spend()` validates funds; `Inventory` and `Shop` keep their collections private and enforce capacity/stock; `Player.getUnlockedAreas()` is read-only.
- **Inheritance:** `Player`/`NPC` extend `Entity`; `Vendor`/`QuestGiver` extend `NPC`; `DebugQuest` extends `Quest`; `Consumable`/`Tool`/`Souvenir` extend `Item`.
- **Polymorphism:** `Item.use()` and `Item.getUnusableReason()` differ per subclass; `Interactable.interact()` returns a shop for a `Vendor` and dialogue for a `QuestGiver`; `Quest.interact()` turns in a fetch quest, while `DebugQuest` overrides it to open a puzzle; `Entity.render()` draws a "!" for vendors and a "?" for quest givers.
- **Abstraction:** `Entity`, `NPC`, `Item` are abstract; `Interactable`, `Renderer` and the DAO interfaces hide how things are drawn or stored.

### Class diagram

```mermaid
classDiagram
  class Entity { <<abstract>> #int x #int y #String sprite +update() +render(Renderer)* }
  class Player { -int id -int money -int stamina -Inventory inventory +move(dx,dy) +spend(amount) boolean +earn(amount) +unlockArea(String) +isExhausted() boolean }
  class NPC { <<abstract>> -String dialogue +interact(Player)* InteractionResult }
  class Vendor { -int id -String name -Shop shop +interact(Player) InteractionResult }
  class QuestGiver { -int npcId -Quest quest +interact(Player) InteractionResult }
  class Interactable { <<interface>> +interact(Player) InteractionResult }
  class Item { <<abstract>> -int id -String name -int price +use(Player)* String +getCategory()* ItemCategory +getUnusableReason(Player) String +isConsumedOnUse() boolean }
  class Consumable { -int staminaRestore }
  class Tool { -String unlocksArea }
  class Souvenir
  class Inventory { -List~Item~ items -int capacity +addItem(Item) boolean +removeItem(Item) boolean +countOf(Item) int }
  class Shop { -Map~Item,Integer~ stock +addStock(Item,int) +buyItem(Player,Item) PurchaseResult }
  class Quest { -int id -String description -int reward -String requiredItemName +complete(Player) boolean +interact(Player,String) InteractionResult }
  class DebugQuest { -String snippet -List~String~ options -int correctIndex +answer(Player,int) boolean }
  class InteractionResult { <<value>> kind: DIALOGUE or SHOP or PUZZLE text shop puzzle }
  class Renderer { <<interface>> +setCamera() +drawTile() +drawSprite() +drawLabel() +fillRect() +drawText() }
  Entity <|-- Player
  Entity <|-- NPC
  NPC <|-- Vendor
  NPC <|-- QuestGiver
  Interactable <|.. Vendor
  Interactable <|.. QuestGiver
  Item <|-- Consumable
  Item <|-- Tool
  Item <|-- Souvenir
  Player "1" --> "1" Inventory
  Vendor "1" --> "1" Shop
  Shop "1" --> "*" Item : stock
  QuestGiver "1" --> "1" Quest
  Quest <|-- DebugQuest
  Entity ..> Renderer : draws with
  Interactable ..> InteractionResult : returns
```

### State diagram: app flow (main menu)

```mermaid
stateDiagram-v2
  [*] --> MENU
  MENU --> PLAYING: Continue (save exists) / New Game (no save)
  MENU --> CONFIRM_NEW: New Game (save exists)
  CONFIRM_NEW --> PLAYING: Yes
  CONFIRM_NEW --> MENU: No / X
  MENU --> SETTINGS: Settings
  SETTINGS --> CONFIRM_ERASE: Erase saved game (save exists)
  CONFIRM_ERASE --> SETTINGS: Yes (erased) / No / X
  SETTINGS --> MENU: Back / X
  PLAYING --> MENU: X or Esc while walking (saves if autosave on)
  MENU --> [*]: Quit
```

### Sequence diagram: player buys an item

```mermaid
sequenceDiagram
  participant S as GameSession
  participant V as Vendor
  participant Sh as Shop
  participant P as Player
  participant I as Inventory
  S->>V: interact(player)
  V-->>S: InteractionResult.shop(greeting, shop)
  Note over S: Screen = SHOP, player picks an item
  S->>Sh: buyItem(player, item)
  Sh->>P: spend(item.price)
  alt not enough money
    P-->>Sh: false
    Sh-->>S: INSUFFICIENT_FUNDS
  else enough money
    P-->>Sh: true
    Sh->>I: addItem(item)
    alt inventory full
      I-->>Sh: false
      Sh->>P: earn(item.price) (refund)
      Sh-->>S: INVENTORY_FULL
    else added
      I-->>Sh: true
      Sh-->>S: SUCCESS (stock decreased)
    end
  end
```

## Database

The same schema and DAOs run on MySQL or on the built-in H2 database (H2 in `MODE=MySQL`), so there is one
set of SQL for both. 
`players`, `items`, `inventory`, `vendors`, `shop_stock`, `quests` as in the original plan, plus a few additions
the game needs (see `docs/BUILD_LOG.md`): table `unlocked_areas`, and columns `items.unlocks_area`,
`quests.required_item`, `vendors.sprite`, `vendors.dialogue`. Milestone 4 adds `debug_quests` (snippet, correct answer) and `quest_options` (the choices, in order), kept in their own tables so a database created earlier gains them automatically. The DAO layer (`Repository<T>`: `save`,
`findById`, `findAll`, `delete`) has an in-memory and a JDBC implementation; both pass the same contract
tests (`DaoContractTest`).

## Editing the island

`src/main/resources/maps/island.txt` is plain text, one character per tile (80x52 now):
`g` grass, `s` sand, `w` water (blocked), `p` path, `t` palm (blocked), `h` hut (blocked),
`j` jungle (needs the Jungle Path). Tests check that every tile you can walk on can be reached from the start
(and that only Yara's jungle pocket needs the machete), so an accidental cut-off island fails the build.

To regenerate or enlarge the world, edit and run `java tools/GenerateMap.java` (it grows `tools/island-40x26.txt`, the
original island, keeping every original coordinate). Add NPCs in `SeedData` on tiles the generator leaves as grass or path.

## Project docs

- `docs/PACKAGING.md`: how to build one runnable jar or a double-click app so anyone can play

- `docs/superpowers/specs/2026-10-04-isla-design.md`: approved design
- `docs/superpowers/plans/2026-10-04-isla-milestones-1-3.md`: implementation plan
- `docs/BUILD_LOG.md`: what was built, decisions, verification, limitations

## Roadmap

- [x] Milestone 4: `DebugQuest extends Quest` (multiple-choice debugging puzzle for the teacher's requirement)
- [x] Milestone 5a: main menu (Continue / New Game / Settings / Quit)
- [ ] Milestone 5b: real art, Figma prototype, pitch deck
