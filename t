warning: in the working copy of 'README.md', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'docs/BUILD_LOG.md', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/isla/game/AppFlow.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/isla/game/GameBootstrap.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/isla/game/SeedData.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/isla/game/Settings.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/isla/ui/IslaApp.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/resources/maps/island.txt', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/test/java/isla/game/AppFlowTest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/test/java/isla/game/GameBootstrapTest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/test/java/isla/game/SettingsTest.java', LF will be replaced by CRLF the next time Git touches it
[1mdiff --git a/README.md b/README.md[m
[1mindex a3570d4..19b5f41 100644[m
[1m--- a/README.md[m
[1m+++ b/README.md[m
[36m@@ -76,13 +76,16 @@[m [mmvn test -Disla.test.jdbcUrl=jdbc:mysql://localhost:3306/isla_test \[m
 ## How the game works[m
 [m
 - **Main menu:** *Continue* loads your save (greyed out when there is none), *New Game* starts over (and asks[m
[31m-  "Overwrite your saved game?" first, defaulting to No), *Settings*, *Quit*.[m
[32m+[m[32m  "Overwrite your saved game?" first, defaulting to No), *How to Play* (goal and controls), *Settings*, *Quit*.[m
 - **Settings:** *Autosave on close* On/Off (remembered in `settings.properties`) and *Erase saved game* (asks first).[m
 [m
 - Start with $50, 100 stamina, 6 inventory slots. Walking costs 1 stamina per 10 steps; at 0 you must rest (R) or use a consumable.[m
 - Vendors (`!`) sell items with limited stock. Buying is blocked without enough money or with a full inventory (you are never charged for nothing).[m
 - **Elder Kai** (`?`) wants a Coconut (sold by Mara for $8) and pays $25 once.[m
[31m-- **Coder Nia** (teal square, below the main road near the start) has broken code: `if (stamina = 0) { rest(); }`. Pick the fix from four options. A wrong pick costs nothing and you can retry; the right one pays $40 once.[m
[32m+[m[32m- **Coder Nia** (teal square, below the main road near the start) has a broken line of code. Pick the fix from four options. A wrong pick costs nothing and you can retry; the right one pays $40 once.[m
[32m+[m[32m  Every **new game draws a different puzzle** from a bank of 13 (never the one you had last time) with the answers shuffled; **Continue keeps your puzzle**.[m
[32m+[m[32m  Add your own in `DebugQuestBank.java` (one entry each).[m
[32m+[m[32m- The world is now **80x52**: the home island, a village island to the east (follow the road across the bridge) and a big island to the south (the bridge at the bottom of the home island).[m
 - The **Rusty Machete** (Tomas, $30) is a Tool: use it from the inventory to unlock the **Jungle Path**, which hides Yara's rare items.[m
 [m
 ## Architecture[m
[36m@@ -99,7 +102,7 @@[m [misla.ui         JavaFX only: IslaApp, JavaFxRenderer, KeyBindings[m
 ```[m
 [m
 Only `isla.ui` imports JavaFX. Everything else draws through the small `Renderer` interface, so the whole game[m
[31m-is unit-tested without a window (210 tests).[m
[32m+[m[32mis unit-tested without a window (243 tests).[m
 [m
 ### OOP pillars[m
 [m
[36m@@ -206,12 +209,18 @@[m [mtests (`DaoContractTest`).[m
 [m
 ## Editing the island[m
 [m
[31m-`src/main/resources/maps/island.txt` is plain text, one character per tile:[m
[32m+[m[32m`src/main/resources/maps/island.txt` is plain text, one character per tile (80x52 now):[m
 `g` grass, `s` sand, `w` water (blocked), `p` path, `t` palm (blocked), `h` hut (blocked),[m
[31m-`j` jungle (needs the Jungle Path). A test checks that every NPC stays reachable (the jungle trader only after unlocking).[m
[32m+[m[32m`j` jungle (needs the Jungle Path). Tests check that every tile you can walk on can be reached from the start[m
[32m+[m[32m(and that only Yara's jungle pocket needs the machete), so an accidental cut-off island fails the build.[m
[32m+[m
[32m+[m[32mTo regenerate or enlarge the world, edit and run `java tools/GenerateMap.java` (it grows `tools/island-40x26.txt`, the[m
[32m+[m[32moriginal island, keeping every original coordinate). Add NPCs in `SeedData` on tiles the generator leaves as grass or path.[m
 [m
 ## Project docs[m
 [m
[32m+[m[32m- `docs/PACKAGING.md`: how to build one runnable jar or a double-click app so anyone can play[m
[32m+[m
 - `docs/superpowers/specs/2026-10-04-isla-design.md`: approved design[m
 - `docs/superpowers/plans/2026-10-04-isla-milestones-1-3.md`: implementation plan[m
 - `docs/BUILD_LOG.md`: what was built, decisions, verification, limitations[m
[1mdiff --git a/docs/BUILD_LOG.md b/docs/BUILD_LOG.md[m
[1mindex 0faa3d2..f0c7b71 100644[m
[1m--- a/docs/BUILD_LOG.md[m
[1m+++ b/docs/BUILD_LOG.md[m
[36m@@ -130,3 +130,31 @@[m [mSaving used to need a MySQL server; without one the game ran from memory and cou[m
 2. **Data folder `isla-data/` in the working directory**, next to `db.properties`.[m
 3. **MySQL down falls back to the built-in database** (with a notice) instead of memory. Saves are not copied between the two.[m
 4. **One Isla window at a time** can use the built-in database (H2 locks the file); a second one falls back to memory with a notice.[m
[32m+[m
[32m+[m[32m---[m
[32m+[m
[32m+[m[32m# Expansion: bigger map, a different puzzle every game, easier to share[m
[32m+[m
[32m+[m[32mBuilt test-first; tests went from 210 to 243.[m
[32m+[m
[32m+[m[32m## What was built[m
[32m+[m[32m| Piece | Delivered |[m
[32m+[m[32m|---|---|[m
[32m+[m[32m| Different debug questions | `DebugQuestBank`: 13 puzzles, answers shuffled. Every new game draws one at random, never the same as the previous game; Continue keeps the saved one. `GameBootstrap.start(repos, newGame, Random)` takes the random source (tests use fixed seeds). Nia's intro line is now generic |[m
[32m+[m[32m| Bigger map | 80x52 world: home island unchanged, east village island and south island joined by bridges. Built by `tools/GenerateMap.java` from `tools/island-40x26.txt`. `BigIslandTest` checks size, amount of land and that all land is reachable |[m
[32m+[m[32m| Playable by anyone | *How to Play* screen on the main menu; `AppPaths` (files live in `~/.isla` unless run from the project folder, so double-clicking works anywhere); `Settings.save` creates missing folders; `isla.Launcher` main class for a runnable jar; `docs/PACKAGING.md` |[m
[32m+[m
[32m+[m[32m## Verification evidence[m
[32m+[m[32m- 243 tests pass (H2 default). The overview of the whole map and in-game screenshots of the east island and the How to Play screen were checked by eye.[m
[32m+[m[32m- The real app was launched through `isla.Launcher` from an unrelated folder with a throwaway home directory: nothing was created in that folder and the database appeared in `~/.isla/isla-data`.[m
[32m+[m
[32m+[m[32m## Not verified here (please check on your machine)[m
[32m+[m[32m- `docs/PACKAGING.md` (maven-shade, jpackage) was **not run**: no access to Maven Central. The shade plugin config is deliberately **not** in `pom.xml` so it cannot break your build; paste it in when you are ready to package.[m
[32m+[m[32m- Walking across the bridges with the real keyboard, and a Windows/macOS double-click launch.[m
[32m+[m[32m- Not done: typing a player name, or several separate players on one computer (one save per computer/folder for now). The new islands have no shops or NPCs yet, only roads, huts and palms.[m
[32m+[m
[32m+[m[32m## Decisions made while building[m
[32m+[m[32m1. **Old coordinates are untouched** (the new land attaches to the east and south), so every NPC position and existing test still holds.[m
[32m+[m[32m2. **A new game never repeats the previous puzzle**, but a continued game keeps its puzzle so progress and question stay consistent.[m
[32m+[m[32m3. **Reward stays $40 for every puzzle** so earning stays predictable and the existing tests still hold.[m
[32m+[m[32m4. **Data location:** the project folder (when it has `pom.xml`, `isla-data`, `db.properties` or `settings.properties`), otherwise `~/.isla`.[m
[1mdiff --git a/src/main/java/isla/game/AppFlow.java b/src/main/java/isla/game/AppFlow.java[m
[1mindex ab0cb42..dddd9d3 100644[m
[1m--- a/src/main/java/isla/game/AppFlow.java[m
[1m+++ b/src/main/java/isla/game/AppFlow.java[m
[36m@@ -14,10 +14,10 @@[m [mimport java.util.List;[m
  */[m
 public class AppFlow {[m
 [m
[31m-    public enum Mode { MENU, SETTINGS, CONFIRM_NEW, CONFIRM_ERASE, PLAYING }[m
[32m+[m[32m    public enum Mode { MENU, HELP, SETTINGS, CONFIRM_NEW, CONFIRM_ERASE, PLAYING }[m
 [m
     public enum MenuItem {[m
[31m-        CONTINUE("Continue"), NEW_GAME("New Game"), SETTINGS("Settings"), QUIT("Quit");[m
[32m+[m[32m        CONTINUE("Continue"), NEW_GAME("New Game"), HOW_TO_PLAY("How to Play"), SETTINGS("Settings"), QUIT("Quit");[m
 [m
         private final String label;[m
 [m
[36m@@ -136,6 +136,7 @@[m [mpublic class AppFlow {[m
         message = "";[m
         switch (mode) {[m
             case MENU -> handleMenu(action);[m
[32m+[m[32m            case HELP -> handleHelp(action);[m
             case SETTINGS -> handleSettings(action);[m
             case CONFIRM_NEW -> handleConfirm(action, this::confirmNewGame, Mode.MENU);[m
             case CONFIRM_ERASE -> handleConfirm(action, this::confirmErase, Mode.SETTINGS);[m
[36m@@ -169,6 +170,7 @@[m [mpublic class AppFlow {[m
                     startPlaying(true);[m
                 }[m
             }[m
[32m+[m[32m            case HOW_TO_PLAY -> mode = Mode.HELP;[m
             case SETTINGS -> {[m
                 settingsCursor = 0;[m
                 mode = Mode.SETTINGS;[m
[36m@@ -177,6 +179,12 @@[m [mpublic class AppFlow {[m
         }[m
     }[m
 [m
[32m+[m[32m    private void handleHelp(Action action) {[m
[32m+[m[32m        if (action == Action.CONFIRM || action == Action.CANCEL) {[m
[32m+[m[32m            mode = Mode.MENU; // the cursor stays on "How to Play"[m
[32m+[m[32m        }[m
[32m+[m[32m    }[m
[32m+[m
     private void handleSettings(Action action) {[m
         switch (action) {[m
             case UP -> settingsCursor = (settingsCursor - 1 + SETTINGS_ENTRIES) % SETTINGS_ENTRIES;[m
[36m@@ -297,6 +305,7 @@[m [mpublic class AppFlow {[m
         drawBackdrop(r);[m
         switch (mode) {[m
             case MENU -> renderMenu(r);[m
[32m+[m[32m            case HELP -> renderHelp(r);[m
             case SETTINGS -> renderSettings(r);[m
             case CONFIRM_NEW -> renderConfirm(r, "Overwrite your saved game?");[m
             case CONFIRM_ERASE -> renderConfirm(r, "Erase your saved game?");[m
[36m@@ -329,7 +338,7 @@[m [mpublic class AppFlow {[m
         r.drawText(222, 46, "ISLA", GameSession.INK);[m
         r.drawText(150, 66, "A 2D Island Trading Adventure", GameSession.INK);[m
 [m
[31m-        panel(r, 140, 92, 200, 118);[m
[32m+[m[32m        panel(r, 140, 88, 200, 122);[m
         boolean canContinue = isContinueAvailable();[m
         for (int i = 0; i < MENU_ITEMS.size(); i++) {[m
             MenuItem item = MENU_ITEMS.get(i);[m
[36m@@ -338,12 +347,34 @@[m [mpublic class AppFlow {[m
             if (unavailable) {[m
                 label += " (no saved game)";[m
             }[m
[31m-            r.drawText(156, 120 + i * 24, cursorLabel(i == menuCursor, label),[m
[32m+[m[32m            r.drawText(156, 118 + i * 20, cursorLabel(i == menuCursor, label),[m
                     unavailable ? GameSession.BORDER : GameSession.INK);[m
         }[m
         r.drawText(150, 228, "Up/Down: choose    Z: select", GameSession.INK);[m
     }[m
 [m
[32m+[m[32m    private void renderHelp(Renderer r) {[m
[32m+[m[32m        panel(r, 30, 14, 420, 214);[m
[32m+[m[32m        String[] lines = {[m
[32m+[m[32m            "HOW TO PLAY",[m
[32m+[m[32m            "",[m
[32m+[m[32m            "Goal: earn money, shop wisely and help the islanders.",[m
[32m+[m[32m            "",[m
[32m+[m[32m            "Arrow keys or WASD ... walk",[m
[32m+[m[32m            "Z, Enter or Space ... talk, confirm, buy, use",[m
[32m+[m[32m            "X, Esc or Backspace ... cancel, or back to the menu",[m
[32m+[m[32m            "I ... Inventory          R ... Rest (+10 stamina)",[m
[32m+[m[32m            "F5 ... Save your game",[m
[32m+[m[32m            "",[m
[32m+[m[32m            "! marks a shop, ? marks someone who needs help.",[m
[32m+[m[32m            "Walking uses stamina: rest or eat to get it back.",[m
[32m+[m[32m        };[m
[32m+[m[32m        for (int i = 0; i < lines.length; i++) {[m
[32m+[m[32m            r.drawText(46, 38 + i * 15, lines[i], GameSession.INK);[m
[32m+[m[32m        }[m
[32m+[m[32m        r.drawText(46, 218, "Z or X: back", GameSession.INK);[m
[32m+[m[32m    }[m
[32m+[m
     private void renderSettings(Renderer r) {[m
         panel(r, 100, 40, 280, 150);[m
         r.drawText(120, 66, "SETTINGS", GameSession.INK);[m
[1mdiff --git a/src/main/java/isla/game/GameBootstrap.java b/src/main/java/isla/game/GameBootstrap.java[m
[1mindex a10ddc5..63a77b1 100644[m
[1m--- a/src/main/java/isla/game/GameBootstrap.java[m
[1m+++ b/src/main/java/isla/game/GameBootstrap.java[m
[36m@@ -1,5 +1,6 @@[m
 package isla.game;[m
 [m
[32m+[m[32mimport isla.DebugQuest;[m
 import isla.Item;[m
 import isla.NPC;[m
 import isla.Player;[m
[36m@@ -11,6 +12,7 @@[m [mimport isla.world.GameWorld;[m
 import isla.world.TileMap;[m
 import java.util.ArrayList;[m
 import java.util.List;[m
[32m+[m[32mimport java.util.Random;[m
 [m
 /**[m
  * Starts and saves games. On first run (empty storage) it seeds the item[m
[36m@@ -29,7 +31,12 @@[m [mpublic final class GameBootstrap {[m
      * discarded and the world is reset to its starting state.[m
      */[m
     public static GameSession start(Repositories repos, boolean newGame) {[m
[31m-        seed(repos, newGame);[m
[32m+[m[32m        return start(repos, newGame, new Random());[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    /** Same, with a given source of luck (tests pass a fixed seed to pick Nia's puzzle). */[m
[32m+[m[32m    public static GameSession start(Repositories repos, boolean newGame, Random random) {[m
[32m+[m[32m        seed(repos, newGame, random);[m
 [m
         Player player = repos.players().findById(SeedData.PLAYER_ID).orElseGet(GameBootstrap::newPlayer);[m
         Quest elderQuest = repos.quests().findById(SeedData.elderQuest().getId()).orElseGet(SeedData::elderQuest);[m
[36m@@ -54,7 +61,7 @@[m [mpublic final class GameBootstrap {[m
 [m
     /** Throws away all saved progress and resets shops and quests to their starting state. */[m
     public static void erase(Repositories repos) {[m
[31m-        seed(repos, true);[m
[32m+[m[32m        seed(repos, true, new Random());[m
     }[m
 [m
     /** Writes the player, every vendor's stock and the quest state to storage. */[m
[36m@@ -76,7 +83,7 @@[m [mpublic final class GameBootstrap {[m
         return p;[m
     }[m
 [m
[31m-    private static void seed(Repositories repos, boolean overwrite) {[m
[32m+[m[32m    private static void seed(Repositories repos, boolean overwrite, Random random) {[m
         if (overwrite) {[m
             repos.players().delete(SeedData.PLAYER_ID);[m
         }[m
[36m@@ -91,10 +98,16 @@[m [mpublic final class GameBootstrap {[m
             }[