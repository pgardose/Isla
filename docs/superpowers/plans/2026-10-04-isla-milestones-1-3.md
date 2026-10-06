# Isla Milestones 1-3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (no subagent tool available) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A playable JavaFX island game (walk, trade, fetch-quest) that saves and restores progress through a MySQL-backed DAO layer.

**Architecture:** The model (`isla`) and world/game logic (`isla.world`, `isla.game`) are pure Java with no JavaFX imports; they draw through a small `Renderer` interface. `isla.ui` is a thin JavaFX shell that maps keys to `Action`s and implements `Renderer`. Persistence is behind DAO interfaces with in-memory and JDBC implementations that pass the same contract tests.

**Tech Stack:** Java 21, JavaFX 21, JUnit 5, MySQL via JDBC (MySQL Connector/J), Maven.

**Spec:** `docs/superpowers/specs/2026-10-04-isla-design.md`

## Global Constraints
- Rendering is JavaFX; art is colored-square placeholders (hex color strings), swappable later.
- Database is MySQL via JDBC behind DAO interfaces; the game must also run with no database installed (in-memory fallback).
- Packages: `isla`, `isla.world`, `isla.game`, `isla.dao`, `isla.ui`. Only `isla.ui` may import `javafx.*`.
- Logical screen 480x272 px, tile 16 px, viewport 30x17 tiles, drawn at 2x.
- Single player, one island; 3 item categories (Consumable, Tool, Souvenir).
- New game: $50, max stamina 100, inventory capacity 6.

## Review Focus
1. Map text with an unknown character or ragged rows fails with a clear `IllegalArgumentException` (Task 2).
2. Walking into the map edge, water, an NPC or locked jungle is blocked and does not move or drain stamina (Task 3).
3. Buying with exactly enough money succeeds; with a full inventory the player is not charged; last unit leaves the shop (Task 5).
4. Turning in a fetch quest without the item gives no reward; turning it in twice pays once (Task 6).
5. Loading with no save starts a new game; save then load restores an empty inventory and unlocked areas; DB unavailable falls back to in-memory with a notice (Tasks 7-9).

---

### Task 1: Scaffold and model foundation
**Files:** Create `pom.xml`, `.gitignore`; Modify all of `src/main/java/isla/*.java`; Create `Renderer`, `ItemCategory`, `PurchaseResult`, `InteractionResult`; Delete `Main.java`; Test `src/test/java/isla/{ItemTest,ShopTest,InventoryTest,QuestTest,PlayerTest}.java`
**Interfaces (Produces):**
- `Renderer { setCamera(int col,int row); drawTile(int col,int row,String hex); drawSprite(int col,int row,String spriteKey); drawLabel(int col,int row,String text); fillRect(int px,int py,int w,int h,String hex); drawText(int px,int py,String text,String hex); }`
- `Item(int id,String name,int price)`: `getId()`, `abstract ItemCategory getCategory()`, `abstract String use(Player)`, `boolean isConsumedOnUse()` (false; `Consumable` true); equality by class+id+name+price. Subclasses `Consumable(int,String,int,int staminaRestore)`, `Tool(int,String,int,String unlocksArea)`, `Souvenir(int,String,int)`.
- `Shop`: `addStock(Item,int qty)`, `getStock():List<Item>` (qty>0, insertion order), `quantityOf(Item):int`, `buyItem(Player,Item):PurchaseResult`.
- `Inventory`: `countOf(Item):int`, `findByName(String):Optional<Item>`.
- `Quest(int id,int giverNpcId,String description,int reward,String requiredItemName)`: `isCompletable(Player)`, `complete(Player):boolean`.
- `InteractionResult`: `Kind{DIALOGUE,SHOP}`, `dialogue(String)`, `shop(String,Shop)`. `Interactable.interact(Player):InteractionResult`.
- `Vendor(int id,String name,int x,int y,String sprite,String dialogue,Shop)`, `QuestGiver(int npcId,int x,int y,String sprite,String dialogue,Quest)`.
- `Player(int id,String name,int money,int maxStamina,int capacity)`: `move(dx,dy)` drains 1 stamina per 10 steps; `isExhausted()`, `getMaxStamina()`, `restoreState(int money,int stamina,int x,int y)`.
- `Entity.render(Renderer)`; `NPC`/`Player` draw their sprite; `Vendor`/`QuestGiver` also draw a "!" label.

- [ ] **Step 1: Write failing tests.** `ShopTest`: `buyExactMoney_succeeds`, `insufficientFunds_returnsInsufficientAndNoCharge`, `fullInventory_returnsInventoryFullAndRefunds`, `lastUnit_removesItemFromStock`, `quantityTwo_allowsTwoPurchases`. `QuestTest`: `withoutItem_notCompletable_noReward`, `withItem_completeConsumesItemPaysRewardOnce`, `secondComplete_returnsFalse`. `PlayerTest`: `tenSteps_drainOneStamina`, `exhaustedAtZero`, `spendMoreThanOwned_returnsFalse`. `ItemTest`: `use_returnsMessage_perSubclass`, `equalsById`. `InventoryTest`: `capacityEnforced`, `countOf`.
- [ ] **Step 2: Run** `mvn -q test` (sandbox: `/home/claude/run.sh`). Expected: compile FAIL (new types undefined).
- [ ] **Step 3: Implement** the signatures above; one-line approach: `Shop` keeps `LinkedHashMap<Item,Integer>`; `use()` returns the message instead of printing.
- [ ] **Step 4: Run.** Expected: all PASS.
- [ ] **Step 5: Commit** `feat: model foundation (renderer, item ids, shop quantities, fetch quests)`.

### Task 2: Island map and content
**Files:** Create `world/TileType.java`, `world/TileMap.java`, `src/main/resources/maps/island.txt`, `game/SeedData.java`; Test `world/TileMapTest.java`, `world/IslandReachabilityTest.java`
**Interfaces (Produces):**
- `TileType` enum (symbol, hex color, walkable, requiredArea): GRASS `g`, SAND `s`, WATER `w` (blocked), PATH `p`, PALM `t` (blocked), HUT `h` (blocked), JUNGLE `j` (needs area "Jungle Path"). `TileType.fromSymbol(char)`.
- `TileMap`: `parse(List<String>)`, `loadResource(String)`, `getWidth()`, `getHeight()`, `tileAt(x,y)`, `inBounds(x,y)`, `canWalk(x,y,Set<String> unlocked)`.
- `SeedData`: `items():List<Item>`, `vendors():List<Vendor>` (3 vendors incl. one behind the jungle), `elder():QuestGiver`, `SPAWN_X/SPAWN_Y`, `NEW_GAME_MONEY=50`, `MAX_STAMINA=100`, `INVENTORY_CAPACITY=6`.

- [ ] **Step 1: Write failing tests.** `TileMapTest`: `parse_unknownSymbol_throwsWithLineAndColumn`, `parse_raggedRows_throws`, `canWalk_outOfBounds_false`, `canWalk_water_false`, `canWalk_jungle_onlyWhenAreaUnlocked`. `IslandReachabilityTest`: `spawn_reachesAllNpcsWithoutJungle_exceptJungleTrader`, `jungleTrader_reachableOnlyAfterUnlock`.
- [ ] **Step 2: Run.** Expected: compile FAIL.
- [ ] **Step 3: Implement** `TileType`, `TileMap`, a 40x26 `island.txt` (water border, 3 huts, jungle patch guarding the Jungle Trader), `SeedData` (start $50; Coconut $8 restoring 10; Coconut Water $10 restoring 20; Rusty Machete $30 unlocking "Jungle Path"; souvenirs). Reachability uses BFS on `canWalk`, with NPC tiles blocked and their adjacent tile as the goal.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: island tile map and seed content`.

### Task 3: GameWorld
**Files:** Create `world/GameWorld.java`, `world/MoveResult.java`; Test `world/GameWorldTest.java`
**Interfaces:** Consumes `TileMap`, `Player`, `NPC`. Produces `GameWorld(TileMap,Player,List<NPC>)`, `movePlayer(int dx,int dy):MoveResult{MOVED,BLOCKED,TOO_TIRED}`, `interactableNear():Optional<NPC>` (4-neighbour), `render(Renderer,int viewCols,int viewRows)`.

- [ ] **Step 1: Failing tests.** `moveOntoGrass_moves`, `moveIntoWater_blockedNoStaminaCost`, `moveOffMapEdge_blocked`, `moveIntoNpc_blocked`, `exhaustedPlayer_tooTired_positionUnchanged`, `jungleBlockedUntilToolUsed`, `interactableNear_findsAdjacentNpcOnly`, `render_setsCameraClampedToMap_andDrawsPlayerLast`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** per interfaces; camera = player position centred, clamped to `[0, mapSize-view]`.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: game world movement, collision, interaction lookup`.

### Task 4: GameSession (exploring) and JavaFX shell
**Files:** Create `game/Action.java`, `game/Screen.java`, `game/GameSession.java`, `ui/IslaApp.java`, `ui/JavaFxRenderer.java`, `ui/KeyBindings.java`; Test `game/GameSessionExploringTest.java`
**Interfaces:** Produces `Action{UP,DOWN,LEFT,RIGHT,CONFIRM,CANCEL,INVENTORY,REST,SAVE}`, `Screen{EXPLORING,DIALOGUE,SHOP,INVENTORY}`, `GameSession(GameWorld)`: `handle(Action)`, `render(Renderer)`, `getScreen()`, `getMessage()`. `IslaApp` entry `main`.

- [ ] **Step 1: Failing tests.** `directionAction_movesPlayerOneTile`, `blockedMove_setsNoMessage`, `tiredMove_setsTiredMessage`, `render_drawsHudWithMoneyAndStamina` (recording renderer sees both texts).
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** session; `JavaFxRenderer` (Canvas 480x272 scaled 2x, camera offset, sprite palette by key, magenta for unknown); `KeyBindings` (arrows/WASD, Z/Enter confirm, X/Esc cancel, I inventory, R rest, F5 save); `IslaApp` with `AnimationTimer` and 120 ms move cooldown. UI classes are compile-verified only.
- [ ] **Step 4: Run** tests and `javac` of `isla.ui`. Expected: PASS and clean compile.
- [ ] **Step 5: Commit** `feat: exploring session and JavaFX shell (milestone 1)`.

### Task 5: Dialogue and shop screens
**Files:** Modify `game/GameSession.java`; Test `game/GameSessionShopTest.java`
**Interfaces:** Consumes `Vendor.interact -> InteractionResult.shop`, `Shop.buyItem`. Produces CONFIRM next to an NPC opens DIALOGUE or SHOP; SHOP: UP/DOWN cursor, CONFIRM buys, CANCEL leaves; messages: "Bought <name>!", "Not enough money.", "Inventory full.".

- [ ] **Step 1: Failing tests.** `confirmNearVendor_opensShop`, `confirmNearNobody_staysExploring`, `buy_updatesMoneyInventoryAndStock`, `buyWithoutMoney_message_noChange`, `buyWithFullInventory_message_noCharge`, `cancel_returnsToExploring`, `cursorWrapsWithinStock`, `emptyStock_confirmDoesNothing`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** shop and dialogue handling and rendering (item grid, dialogue box at bottom third, money top-right).
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: dialogue and shop screens`.

### Task 6: Inventory screen, item use, rest, fetch quest
**Files:** Modify `game/GameSession.java`; Test `game/GameSessionInventoryTest.java`, `game/GameSessionQuestTest.java`
**Interfaces:** Consumes `Item.use`, `Item.isConsumedOnUse`, `Quest.complete`. Produces INVENTORY screen: LEFT/RIGHT switch category tab, UP/DOWN cursor, CONFIRM uses item, CANCEL/INVENTORY closes; REST restores 10 stamina.

- [ ] **Step 1: Failing tests.** `inventoryKey_opensAndClosesInventory`, `tabsFilterByCategory`, `useConsumable_restoresStaminaAndRemovesItem`, `useTool_unlocksAreaAndKeepsItem`, `useSouvenir_keepsItem`, `emptyTab_confirmDoesNothing`, `rest_restoresStaminaCapped`, `elderWithoutCoconut_showsQuestText_noReward`, `elderWithCoconut_paysOnceAndConsumesItem`, `elderAfterCompletion_thanksOnly`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** inventory handling, grouped counts display, rest.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: inventory, item use, rest, fetch quest (milestone 2)`.

### Task 7: DAO interfaces and in-memory implementations
**Files:** Create `dao/Repository.java`, `dao/{ItemDAO,PlayerDAO,VendorDAO,QuestDAO}.java`, `dao/InMemory{Item,Player,Vendor,Quest}DAO.java`; Test `dao/{ItemDaoContract,PlayerDaoContract,VendorDaoContract,QuestDaoContract}.java` (abstract) and `InMemory*DaoTest.java`
**Interfaces:** Produces `Repository<T>{ void save(T); Optional<T> findById(int); List<T> findAll(); void delete(int); }`; `PlayerDAO(ItemDAO)` saves player row, inventory counts, unlocked areas; `VendorDAO` saves vendor row and stock quantities. In-memory DAOs store copies of the data, never the live object.

- [ ] **Step 1: Failing contract tests.** `save_thenFind_roundTrips` (money, stamina, position, inventory counts, unlocked areas), `emptyInventory_roundTrips`, `find_missing_returnsEmpty`, `save_twice_updates`, `delete_removes`, `vendorStock_quantitiesRoundTrip`, `questCompletion_roundTrips`, `loaded_isIndependentCopy`.
- [ ] **Step 2: Run.** Expected: compile FAIL.
- [ ] **Step 3: Implement** interfaces and in-memory versions.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: DAO interfaces and in-memory implementations`.

### Task 8: JDBC DAOs and schema
**Files:** Create `db/schema.sql`, `dao/Database.java`, `dao/Jdbc{Item,Player,Vendor,Quest}DAO.java`, `src/main/resources/db.properties.example`; Test `dao/Jdbc*DaoTest.java` (subclass the contracts)
**Interfaces:** Produces `Database(String url,String user,String password)`: `connection():Connection`, `initializeSchema()`; `Database.fromProperties(Path)`. Tables per the README plus `unlocked_areas`, plus columns `items.unlocks_area`, `quests.required_item`, `vendors.sprite`, `vendors.dialogue`.

- [ ] **Step 1: Failing tests.** The same contract classes run against a live database when `-Disla.test.jdbcUrl` is set; skipped otherwise (`Assumptions`).
- [ ] **Step 2: Run** against MariaDB. Expected: FAIL (classes missing).
- [ ] **Step 3: Implement** with `INSERT ... ON DUPLICATE KEY UPDATE` and transactions for aggregate saves.
- [ ] **Step 4: Run** against MariaDB. Expected: PASS.
- [ ] **Step 5: Commit** `feat: JDBC DAOs and MySQL schema`.

### Task 9: Save and continue wiring
**Files:** Create `game/GameBootstrap.java`, `game/Repositories.java`; Modify `game/GameSession.java`, `ui/IslaApp.java`; Test `game/GameBootstrapTest.java`
**Interfaces:** Produces `Repositories.inMemory()`, `Repositories.jdbc(Database)`; `GameBootstrap.start(Repositories,boolean newGame):GameSession` (seeds if empty, loads player 1 if present); `GameBootstrap.save(GameSession)`; SAVE action saves with message "Game saved."; `IslaApp` autosaves on close and falls back to in-memory with notice "No database - progress will not be saved."

- [ ] **Step 1: Failing tests.** `noSave_startsNewGame_withSeedStateAndSpawn`, `saveThenStart_restoresMoneyPositionInventoryAreasStockAndQuest`, `newGameFlag_ignoresExistingSave`, `saveAction_savesAndShowsMessage`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement**; seeding writes `SeedData` through the DAOs only when the item table is empty.
- [ ] **Step 4: Run** in-memory and against MariaDB. Expected: PASS.
- [ ] **Step 5: Commit** `feat: save and continue (milestone 3)`.

### Task 10: Documentation and delivery
**Files:** Create `README.md` (setup, run, controls, diagrams), `docs/BUILD_LOG.md`; verify `pom.xml` is well-formed.

- [ ] **Step 1:** Update the README (class diagram, purchase sequence diagram, schema, run instructions) to match the code.
- [ ] **Step 2:** Run the full suite plus a MariaDB run. Expected: all PASS.
- [ ] **Step 3:** Zip the project and present it.
