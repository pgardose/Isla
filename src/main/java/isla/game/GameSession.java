package isla.game;

import isla.DebugQuest;
import isla.InteractionResult;
import isla.Item;
import isla.ItemCategory;
import isla.Journey;
import isla.NPC;
import isla.Player;
import isla.PurchaseResult;
import isla.Quest;
import isla.QuestGiver;
import isla.Renderer;
import isla.Shop;
import isla.world.GameWorld;
import isla.world.MoveResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The game controller. Receives Actions (from keyboard, tests, anything),
 * changes the world, and draws itself through a Renderer. It never imports
 * JavaFX, so the whole game can be unit tested without a window.
 *
 * Which screen is active (exploring, dialogue, shop, ...) decides how an
 * Action is interpreted and what is drawn.
 */
public class GameSession {

    public static final int TILE_SIZE = 16;
    public static final int VIEW_COLS = 30;
    public static final int VIEW_ROWS = 17;
    public static final int SCREEN_WIDTH = VIEW_COLS * TILE_SIZE;   // 480
    public static final int SCREEN_HEIGHT = VIEW_ROWS * TILE_SIZE;  // 272

    /** The closing story shown on the ending screen (wrapped to fit). */
    static final String ENDING_STORY = "The shards lock together with a sound like a bell. The east bridge stands"
            + " solid, the village laughs again and the south jungle settles back to sleep. Nia starts teaching"
            + " the island's children to read the old code, carefully this time. Elder Kai sets the mended Idol"
            + " in its shrine. \"An island runs on its code,\" he says, \"but it is kept by people who tell the"
            + " truth.\"";

    // UI colours. They are the isla.ui.Palette colours (this package may not import isla.ui, so the
    // hex codes are repeated here; PaletteMatchesGameColoursTest fails if they ever drift apart).
    static final String INK = "#2B1D0E";        // Palette.INK
    static final String PAPER = "#F5E9C8";      // Palette.PAPER
    static final String CARD = "#F5E9C8";       // Palette.PAPER
    static final String BORDER = "#B5651D";     // Palette.WOOD
    static final String GOLD = "#F4C542";       // Palette.GOLD
    static final String MONEY_INK = "#B5651D";  // Palette.WOOD
    static final String HUD_FILL = "#1A2733";   // Palette.SHADOW
    static final String HUD_TEXT = "#D6F0F7";   // Palette.FOAM
    static final String BAR_BACK = "#2B1D0E";   // Palette.INK
    static final String BAR_FILL = "#7BC96F";   // Palette.GRASS
    private static final int WRAP_CHARS = 76;
    /** Lines in one page of the dialogue box. */
    static final int PAGE_LINES = 3;
    /** At or below this the stamina bar turns red and the hero feels faint. At 0 they die. */
    public static final int LOW_STAMINA = 20;
    static final String DANGER = "#E8664A";     // Palette.CORAL
    /** What a nap in a villager's hut costs: rest is never free, so stamina and money both matter. */
    public static final int HUT_NAP_PRICE = 10;
    /** Extra stamina lost per step taken next to a broken (unfixed) cursed tile. */
    public static final int GLITCH_STATIC_STAMINA = 2;

    private final GameWorld world;
    private Screen screen = Screen.EXPLORING;
    private String message = "";
    private String dialogueText = "";
    private int dialoguePage;
    private Journey journey;
    private QuestGiver activeGiver;
    private String deathCause = "";
    private boolean faintWarned;
    private boolean checkpoints;
    private int previousBest = -1;
    private Shop activeShop;
    private int shopCursor;
    private DebugQuest activePuzzle;
    private int puzzleCursor;
    private ItemCategory inventoryTab = ItemCategory.CONSUMABLE;
    private int inventoryCursor;
    private Supplier<String> saveHandler = () -> "Saving is not available.";

    public GameSession(GameWorld world) {
        this.world = world;
    }

    /** The main story, for the objective strip and the journal (null in bare test worlds). */
    public Journey getJourney() {
        return journey;
    }

    public void setJourney(Journey journey) {
        this.journey = journey;
    }

    /** Which page of a long dialogue is showing (0 = the first). */
    public int getDialoguePage() {
        return dialoguePage;
    }

    /** How many pages the current dialogue has. */
    public int dialoguePageCount() {
        return Math.max(1, (wrap(dialogueText, WRAP_CHARS).size() + PAGE_LINES - 1) / PAGE_LINES);
    }

    /** True once the hero has died (the GAME_OVER screen); a dead game must never be saved. */
    public boolean isDead() {
        return screen == Screen.GAME_OVER;
    }

    /** How the hero died, for the game-over screen ("" while alive). */
    public String getDeathCause() {
        return deathCause;
    }

    /** When on, the game saves by itself each time a story step is completed (AppFlow turns this on). */
    public void setCheckpoints(boolean on) {
        this.checkpoints = on;
    }

    /** The best score before this run (-1 = none), so the ending can say whether this run beat it. */
    public void setPreviousBest(int points) {
        this.previousBest = points;
    }

    /** This playthrough's score so far: wrong answers and deaths. */
    public Score getScore() {
        return new Score(getPlayer().getWrongAnswers(), getPlayer().getDeaths());
    }

    public GameWorld getWorld() {
        return world;
    }

    public Player getPlayer() {
        return world.getPlayer();
    }

    public Screen getScreen() {
        return screen;
    }

    /** One-line feedback for the last action ("" when there is nothing to say). */
    public String getMessage() {
        return message;
    }

    /** The NPC's line currently shown in the dialogue box (dialogue and shop screens). */
    public String getDialogueText() {
        return dialogueText;
    }

    /** The puzzle on screen; non-null only while the DEBUG_PUZZLE screen is open. */
    public DebugQuest getActivePuzzle() {
        return activePuzzle;
    }

    public int getPuzzleCursor() {
        return puzzleCursor;
    }

    public Shop getActiveShop() {
        return activeShop;
    }

    public int getShopCursor() {
        return shopCursor;
    }

    /**
     * One goal line per quest giver, main quest (Elder Kai) first: the active goal such as "Bring Elder Kai a
     * coconut", or "Done: ..." once that giver has nothing left to ask.
     */
    public List<String> questLines() {
        List<String> lines = new ArrayList<>();
        for (NPC npc : world.getNpcs()) {
            if (npc instanceof QuestGiver giver) {
                lines.add(giver.getGoal());
            }
        }
        return lines;
    }

    /** The lines shown on the ending screen. Worked out from the saved quests, so it is right after Continue too. */
    public List<String> endingSummary() {
        int total = 0;
        int done = 0;
        int earned = 0;
        int stones = 0;
        int stonesFixed = 0;
        for (NPC npc : world.getNpcs()) {
            if (npc instanceof QuestGiver giver) {
                boolean stone = "stone_sprite".equals(giver.getSprite());
                for (Quest q : giver.getQuests()) {
                    total++;
                    if (stone) {
                        stones++;
                    }
                    if (q.isCompleted()) {
                        done++;
                        earned += q.getReward();
                        if (stone) {
                            stonesFixed++;
                        }
                    }
                }
            }
        }
        List<String> lines = new ArrayList<>();
        if (stones > 0) {
            lines.add("Cursed stones repaired: " + stonesFixed + " of " + stones);
            lines.add("Clues found: " + cluesFound().size() + " of " + SeedData.CLUES.size());
        }
        lines.add("Quests completed: " + done + " of " + total + "   Money in hand: $" + getPlayer().getMoney());
        Score score = getScore();
        lines.add("Wrong answers: " + score.wrongAnswers() + "   Deaths: " + score.deaths());
        return lines;
    }

    /** Journal lines for the clues the player has found, in story order. */
    public List<String> cluesFound() {
        List<String> found = new ArrayList<>();
        SeedData.CLUES.forEach((flag, text) -> {
            if (getPlayer().hasUnlocked(flag)) {
                found.add(text);
            }
        });
        return found;
    }

    /** Names of the shards found so far ("Sun", "Moon", "Sea"), in story order. */
    public List<String> shardsFound() {
        List<String> found = new ArrayList<>();
        SeedData.SHARDS.forEach((flag, name) -> {
            if (getPlayer().hasUnlocked(flag)) {
                found.add(name);
            }
        });
        return found;
    }

    /** Side goals the player can do right now (Nia, the villagers), for the journal. */
    public List<String> sideGoals() {
        List<String> goals = new ArrayList<>();
        for (NPC npc : world.getNpcs()) {
            if (npc instanceof QuestGiver g && !g.isFinale() && !"stone_sprite".equals(g.getSprite())) {
                g.currentStage().filter(st -> st.isOpenFor(getPlayer()))
                        .filter(st -> journey == null || journey.getChapters().stream()
                                .noneMatch(c -> c.quest() == st.quest()))
                        .ifPresent(st -> goals.add(st.goal()));
            }
        }
        return goals;
    }

    /** Sets what the SAVE action does; the handler returns the message to show. */
    public void setSaveHandler(Supplier<String> handler) {
        this.saveHandler = handler;
    }

    /** Shows a one-off message (e.g. a start-up notice) until the next action. */
    public void notice(String text) {
        this.message = text;
    }

    public ItemCategory getInventoryTab() {
        return inventoryTab;
    }

    public int getInventoryCursor() {
        return inventoryCursor;
    }

    /** Distinct items in the current inventory tab, in the order first collected. */
    public List<Item> inventoryEntries() {
        List<Item> entries = new ArrayList<>();
        for (Item item : getPlayer().getInventory().getItems()) {
            if (item.getCategory() == inventoryTab && !entries.contains(item)) {
                entries.add(item);
            }
        }
        return entries;
    }

    public void handle(Action action) {
        message = "";
        switch (screen) {
            case EXPLORING -> handleExploring(action);
            case DIALOGUE -> handleDialogue(action);
            case SHOP -> handleShop(action);
            case INVENTORY -> handleInventory(action);
            case DEBUG_PUZZLE -> handlePuzzle(action);
            case QUESTS -> handleQuestList(action);
            case ENDING -> handleEnding(action);
            case GAME_OVER -> { } // AppFlow decides: last save or main menu
        }
    }

    // ---- exploring ----

    private void handleExploring(Action action) {
        switch (action) {
            case UP -> walk(0, -1);
            case DOWN -> walk(0, 1);
            case LEFT -> walk(-1, 0);
            case RIGHT -> walk(1, 0);
            case CONFIRM -> interact();
            case INVENTORY -> openInventory();
            case REST -> rest();
            case SAVE -> save();
            case QUESTS -> screen = Screen.QUESTS;
            default -> { }
        }
    }

    private void walk(int dx, int dy) {
        MoveResult result = world.movePlayer(dx, dy);
        if (result == MoveResult.TOO_TIRED) {
            message = "You are too tired to move. Press R to rest or use a consumable.";
        }
        if (result == MoveResult.MOVED && world.playerNextToActiveGlitch()) {
            Player p = getPlayer();
            p.expendStamina(Math.min(GLITCH_STATIC_STAMINA, p.getStamina()));
            message = "Glitch static stings you! -" + GLITCH_STATIC_STAMINA + " stamina. Fix the stone to stop it.";
            if (p.getStamina() <= LOW_STAMINA && !faintWarned) {
                message += " You feel faint!";
            }
            checkStamina("You lingered too long by the cursed tiles. The glitch static drained the last of your"
                    + " strength.");
            return;
        }
        checkStamina("Your legs gave out far from food and shade. You collapsed from exhaustion and never woke up.");
    }

    /** Dies at 0 stamina; warns once each time stamina drops into the danger zone. */
    private void checkStamina(String causeIfDead) {
        Player p = getPlayer();
        if (p.getStamina() <= 0) {
            deathCause = causeIfDead;
            p.recordDeath();
            activePuzzle = null;
            activeShop = null;
            screen = Screen.GAME_OVER;
            return;
        }
        if (p.getStamina() <= LOW_STAMINA) {
            if (!faintWarned && message.isEmpty()) {
                message = "You feel faint... Eat something (I) or rest next to a hut (R), or you will die!";
            }
            faintWarned = true;
        } else {
            faintWarned = false;
        }
    }

    /** Saves by itself after a story step, if checkpoints are on. Never shows an error over the story text. */
    private void checkpoint() {
        if (!checkpoints) {
            return;
        }
        try {
            saveHandler.get();
        } catch (RuntimeException e) {
            System.err.println("Checkpoint save failed: " + e.getMessage());
        }
    }

    private void interact() {
        NPC npc = world.interactableNear().orElse(null);
        if (npc == null) {
            return;
        }
        Quest before = npc instanceof QuestGiver qg ? qg.currentStage().map(QuestGiver.Stage::quest).orElse(null) : null;
        InteractionResult result = npc.interact(getPlayer());
        dialogueText = result.getText();
        dialoguePage = 0;
        activeGiver = npc instanceof QuestGiver g ? g : null;
        if (before != null && before.isCompleted()) {
            checkpoint(); // a story step was just completed by talking
        }
        switch (result.getKind()) {
            case SHOP -> {
                activeShop = result.getShop();
                shopCursor = 0;
                screen = Screen.SHOP;
            }
            case PUZZLE -> {
                activePuzzle = result.getPuzzle();
                puzzleCursor = 0;
                screen = Screen.DEBUG_PUZZLE;
            }
            case ENDING -> screen = Screen.ENDING;
            default -> screen = Screen.DIALOGUE;
        }
    }

    // ---- quest list and ending ----

    private void handleQuestList(Action action) {
        if (action == Action.CANCEL || action == Action.CONFIRM || action == Action.QUESTS) {
            screen = Screen.EXPLORING;
        }
    }

    /**
     * Z keeps exploring. X (back to the main menu) is handled one level up by AppFlow, because leaving the
     * island means saving and showing the menu, which a session knows nothing about.
     */
    private void handleEnding(Action action) {
        if (action == Action.CONFIRM) {
            screen = Screen.EXPLORING;
        }
    }

    // ---- dialogue ----

    /** Z turns the page (or closes on the last one); X closes at once. */
    private void handleDialogue(Action action) {
        if (action == Action.CONFIRM && dialoguePage + 1 < dialoguePageCount()) {
            dialoguePage++;
        } else if (action == Action.CONFIRM || action == Action.CANCEL) {
            screen = Screen.EXPLORING;
        }
    }

    // ---- shop ----

    private void handleShop(Action action) {
        List<Item> stock = activeShop.getStock();
        switch (action) {
            case UP -> {
                if (!stock.isEmpty()) {
                    shopCursor = (shopCursor - 1 + stock.size()) % stock.size();
                }
            }
            case DOWN -> {
                if (!stock.isEmpty()) {
                    shopCursor = (shopCursor + 1) % stock.size();
                }
            }
            case CONFIRM -> buySelected(stock);
            case CANCEL -> {
                screen = Screen.EXPLORING;
                activeShop = null;
            }
            default -> { }
        }
    }

    private void buySelected(List<Item> stock) {
        if (stock.isEmpty()) {
            message = "Nothing left to buy.";
            return;
        }
        Item item = stock.get(shopCursor);
        PurchaseResult result = activeShop.buyItem(getPlayer(), item);
        message = switch (result) {
            case SUCCESS -> "Bought " + item.getName() + "!";
            case INSUFFICIENT_FUNDS -> "Not enough money.";
            case INVENTORY_FULL -> "Inventory full.";
            case NOT_IN_STOCK -> "Sold out.";
        };
        int remaining = activeShop.getStock().size();
        if (shopCursor >= remaining) {
            shopCursor = Math.max(0, remaining - 1);
        }
    }

    // ---- debug puzzle ----

    private void handlePuzzle(Action action) {
        int n = activePuzzle.getOptions().size();
        switch (action) {
            case UP -> puzzleCursor = (puzzleCursor - 1 + n) % n;
            case DOWN -> puzzleCursor = (puzzleCursor + 1) % n;
            case CONFIRM -> submitPuzzleAnswer();
            case CANCEL -> closePuzzle();
            default -> { }
        }
    }

    /**
     * A right answer completes the quest (paying it and setting its story flags, which is what opens bridges and
     * hands over shards) and shows the giver's line for it; finishing the finale giver shows the ending. A wrong
     * answer explains itself and, for a code fix, costs a little stamina.
     */
    private void submitPuzzleAnswer() {
        DebugQuest puzzle = activePuzzle;
        Player p = getPlayer();
        if (puzzle.answer(p, puzzleCursor)) {
            String text = activeGiver != null ? activeGiver.doneTextFor(puzzle)
                    : "That fixed it! Here is $" + puzzle.getReward() + " for your trouble.";
            if (puzzle.getReward() > 0 && !text.contains("$")) {
                text += " (+$" + puzzle.getReward() + ")";
            }
            dialogueText = text;
            dialoguePage = 0;
            boolean ending = activeGiver != null && activeGiver.isFinale() && activeGiver.isFinished();
            closePuzzle();
            screen = ending ? Screen.ENDING : Screen.DIALOGUE;
            checkpoint();
        } else {
            int cost = Math.min(puzzle.wrongAnswerStaminaCost(), p.getStamina());
            p.expendStamina(cost);
            p.recordWrongAnswer();
            message = puzzle.feedbackFor(puzzleCursor)
                    + (cost > 0 ? " Glitch shock! -" + cost + " stamina." : "");
            if (cost > 0) {
                faintWarned = true; // the shock message already says enough
                checkStamina("The cursed stone's glitch shock was too much for your exhausted body.");
            }
        }
    }

    private void closePuzzle() {
        activePuzzle = null;
        screen = Screen.EXPLORING;
    }

    // ---- save ----

    private void save() {
        try {
            message = saveHandler.get();
        } catch (RuntimeException e) {
            message = "Save failed: " + e.getMessage();
        }
    }

    // ---- rest ----

    /**
     * Resting needs a hut: for $10 the owner lets you sleep and you wake fully rested. Out in the open there is no
     * rest at all, so walking far without food or money is dangerous: at 0 stamina the hero dies.
     */
    private void rest() {
        Player p = getPlayer();
        if (world.playerNextToHut()) {
            if (p.getStamina() >= p.getMaxStamina()) {
                message = "You are already well rested.";
                return;
            }
            if (!p.spend(HUT_NAP_PRICE)) {
                message = "The hut owner wants $" + HUT_NAP_PRICE + " for a nap. You can't pay. Find food!";
                return;
            }
            p.restoreStamina(p.getMaxStamina());
            message = "You pay $" + HUT_NAP_PRICE + " and nap in the hut. Stamina fully restored!";
        } else if (p.getStamina() >= p.getMaxStamina()) {
            message = "You are not tired.";
        } else {
            message = "No shade here. Rest next to a hut, or eat something (I).";
        }
    }

    // ---- inventory ----

    private void openInventory() {
        screen = Screen.INVENTORY;
        inventoryTab = ItemCategory.CONSUMABLE;
        inventoryCursor = 0;
    }

    private void handleInventory(Action action) {
        ItemCategory[] tabs = ItemCategory.values();
        List<Item> entries = inventoryEntries();
        switch (action) {
            case LEFT -> changeTab((inventoryTab.ordinal() - 1 + tabs.length) % tabs.length);
            case RIGHT -> changeTab((inventoryTab.ordinal() + 1) % tabs.length);
            case UP -> {
                if (!entries.isEmpty()) {
                    inventoryCursor = (inventoryCursor - 1 + entries.size()) % entries.size();
                }
            }
            case DOWN -> {
                if (!entries.isEmpty()) {
                    inventoryCursor = (inventoryCursor + 1) % entries.size();
                }
            }
            case CONFIRM -> useSelected(entries);
            case CANCEL, INVENTORY -> screen = Screen.EXPLORING;
            default -> { }
        }
    }

    private void changeTab(int ordinal) {
        inventoryTab = ItemCategory.values()[ordinal];
        inventoryCursor = 0;
    }

    private void useSelected(List<Item> entries) {
        if (entries.isEmpty()) {
            return;
        }
        Item item = entries.get(inventoryCursor);
        String reason = item.getUnusableReason(getPlayer());
        if (reason != null) {
            message = reason;
            return;
        }
        message = item.use(getPlayer());
        if (item.isConsumedOnUse()) {
            getPlayer().getInventory().removeItem(item);
        }
        int remaining = inventoryEntries().size();
        if (inventoryCursor >= remaining) {
            inventoryCursor = Math.max(0, remaining - 1);
        }
    }

    // ---- drawing ----

    public void render(Renderer r) {
        world.render(r, VIEW_COLS, VIEW_ROWS);
        if (screen == Screen.EXPLORING || screen == Screen.DIALOGUE) {
            renderHud(r); // shop and inventory panels show their own money/capacity
            renderObjective(r);
        }
        switch (screen) {
            case EXPLORING -> {
                if (!message.isEmpty()) {
                    renderMessageBox(r, message);
                }
            }
            case DIALOGUE -> renderDialoguePage(r);
            case SHOP -> renderShop(r);
            case INVENTORY -> renderInventory(r);
            case DEBUG_PUZZLE -> renderPuzzle(r);
            case QUESTS -> renderQuestList(r);
            case ENDING -> renderEnding(r);
            case GAME_OVER -> renderGameOver(r);
        }
    }

    private void renderGameOver(Renderer r) {
        r.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, HUD_FILL);
        r.drawPanel(40, 40, 400, 192, PAPER, DANGER);
        r.drawText(204, 70, "YOU DIED", DANGER);
        List<String> cause = wrap(deathCause, 60);
        for (int i = 0; i < cause.size(); i++) {
            r.drawText(56, 98 + i * 14, cause.get(i), INK);
        }
        r.drawText(56, 150, "Tip: eat food (I), or pay $" + HUT_NAP_PRICE + " to rest at a hut (R).", BORDER);
        r.drawText(56, 164, "The bar turns red at " + LOW_STAMINA + " stamina or less: act fast.", BORDER);
        r.drawText(56, 196, "Z: wake up at your last save, fully rested", INK);
        r.drawText(56, 214, "X: back to the main menu", INK);
    }

    private void renderDialoguePage(Renderer r) {
        List<String> lines = wrap(dialogueText, WRAP_CHARS);
        int from = Math.min(dialoguePage * PAGE_LINES, Math.max(0, lines.size() - 1));
        List<String> page = lines.subList(from, Math.min(lines.size(), from + PAGE_LINES));
        int pages = dialoguePageCount();
        String hint = dialoguePage + 1 < pages
                ? "Z: next (" + (dialoguePage + 1) + "/" + pages + ")   X: skip" : "Z: close";
        renderDialogueBox(r, page, hint);
    }

    /** The "what now?" strip at the top of the screen, so the player always knows the next step. */
    private void renderObjective(Renderer r) {
        if (journey == null) {
            return;
        }
        r.drawPanel(128, 4, 348, 20, HUD_FILL, BORDER);
        r.drawText(134, 17, journey.objectiveText(), GOLD);
    }

    /** The journal (Q): chapter, goal, shards, clues and side goals; a plain quest list in bare test worlds. */
    private void renderQuestList(Renderer r) {
        if (journey == null) {
            r.drawPanel(20, 12, 440, 112, PAPER, BORDER);
            r.drawText(30, 32, "QUESTS", INK);
            List<String> lines = questLines();
            for (int i = 0; i < lines.size(); i++) {
                String label = i == 0 ? "Goal: " : "Side: ";
                r.drawText(30, 56 + i * 18, label + lines.get(i), INK);
            }
            r.drawText(30, 112, "Q or X: close", BORDER);
            return;
        }
        r.drawPanel(20, 8, 440, 256, PAPER, BORDER);
        r.drawText(30, 26, "JOURNAL - " + journey.partText(), INK);
        r.drawText(30, 44, "Goal: " + journey.objectiveText(), MONEY_INK);

        List<String> shards = shardsFound();
        r.drawText(30, 64, "Idol shards (" + shards.size() + " of " + SeedData.SHARDS.size() + "):", INK);
        int sx = 190;
        for (String name : SeedData.SHARDS.values()) {
            boolean have = shards.contains(name);
            r.fillRect(sx, 54, 74, 14, have ? GOLD : BORDER);
            r.fillRect(sx + 2, 56, 70, 10, have ? GOLD : PAPER);
            r.drawText(sx + 8, 65, have ? name : "?", INK);
            sx += 82;
        }

        r.drawText(30, 88, "Clues", INK);
        List<String> clues = cluesFound();
        if (clues.isEmpty()) {
            r.drawText(40, 104, "None yet. Fix stones and talk to people.", BORDER);
        }
        for (int i = 0; i < clues.size(); i++) {
            r.drawText(40, 104 + i * 14, "- " + clues.get(i), INK);
        }

        int y = 104 + Math.max(1, clues.size()) * 14 + 12;
        List<String> side = sideGoals();
        if (!side.isEmpty()) {
            r.drawText(30, y, "Side", INK);
            for (int i = 0; i < side.size() && i < 3; i++) {
                r.drawText(40, y + 16 + i * 14, "- " + side.get(i), INK);
            }
        }
        Score score = getScore();
        r.drawText(30, 238, "Wrong answers: " + score.wrongAnswers() + "   Deaths: " + score.deaths()
                + "   Rank so far: " + score.rank(), INK);
        r.drawText(30, 254, "Story progress: " + journey.completedCount() + " of " + journey.size()
                + " steps    Q or X: close", BORDER);
    }

    private void renderEnding(Renderer r) {
        r.drawPanel(20, 8, 440, 256, PAPER, BORDER);
        r.drawText(214, 28, "THE END", INK);
        List<String> story = wrap(ENDING_STORY, 68);
        for (int i = 0; i < story.size(); i++) {
            r.drawText(34, 48 + i * 13, story.get(i), INK);
        }
        int y = 48 + story.size() * 13 + 8;
        r.drawText(34, y, "Your story", MONEY_INK);
        List<String> summary = endingSummary();
        for (int i = 0; i < summary.size(); i++) {
            r.drawText(46, y + 14 + i * 13, summary.get(i), INK);
        }
        Score score = getScore();
        int ry = y + 14 + summary.size() * 13 + 6;
        r.fillRect(34, ry - 11, 412, 16, GOLD);
        r.drawText(42, ry, "RANK " + score.rank() + "  " + score.title() + "   " + score.points() + " points", INK);
        String best = previousBest < 0 ? "Your first finished story!"
                : score.points() > previousBest ? "New personal best! (was " + previousBest + ")"
                : "Best so far: " + previousBest + " (" + Score.rankFor(previousBest, previousBest == Score.START)
                        + "). Play again to beat it!";
        r.drawText(42, ry + 16, best, MONEY_INK);
        r.drawText(34, 252, "Z: keep exploring     X: back to the main menu (saves first)", INK);
    }

    private void renderHud(Renderer r) {
        Player p = getPlayer();
        r.drawPanel(4, 4, 120, 44, HUD_FILL, BORDER);
        r.drawText(10, 17, "$" + p.getMoney(), GOLD);
        r.drawText(10, 29, "STA " + p.getStamina() + "/" + p.getMaxStamina(), HUD_TEXT);
        r.fillRect(10, 34, 108, 8, BAR_BACK);
        int filled = p.getMaxStamina() == 0 ? 0 : 108 * p.getStamina() / p.getMaxStamina();
        r.fillRect(10, 34, filled, 8, p.getStamina() <= LOW_STAMINA ? DANGER : BAR_FILL);
    }

    private void renderShop(Renderer r) {
        r.drawPanel(20, 12, 440, 182, PAPER, BORDER);
        r.drawText(30, 30, "FOR SALE", INK);
        r.drawText(396, 30, "$" + getPlayer().getMoney(), MONEY_INK);

        List<Item> stock = activeShop.getStock();
        if (stock.isEmpty()) {
            r.drawText(30, 64, "Sold out - nothing left to buy.", INK);
        }
        for (int i = 0; i < stock.size(); i++) {
            Item item = stock.get(i);
            int cx = 30 + (i % 2) * 214;
            int cy = 40 + (i / 2) * 46;
            r.fillRect(cx, cy, 204, 40, i == shopCursor ? GOLD : BORDER);
            r.fillRect(cx + 2, cy + 2, 200, 36, CARD);
            r.drawText(cx + 8, cy + 17, item.getName(), INK);
            r.drawText(cx + 8, cy + 32, "$" + item.getPrice() + "  x" + activeShop.quantityOf(item)
                    + "  [" + item.getCategory().getLabel() + "]", MONEY_INK);
        }
        String text = message.isEmpty() ? dialogueText : message;
        renderDialogueBox(r, wrap(text, WRAP_CHARS), "Z: buy   X: leave");
    }

    private void renderPuzzle(Renderer r) {
        r.drawPanel(20, 12, 440, 182, PAPER, BORDER);
        r.drawText(30, 30, activePuzzle.getHeading(), INK);
        if (activePuzzle.getReward() > 0) {
            r.drawText(396, 30, "$" + activePuzzle.getReward(), MONEY_INK);
        }
        Player p = getPlayer();
        if (activePuzzle.wrongAnswerStaminaCost() > 0) {
            r.drawText(300, 30, "STA " + p.getStamina(), p.getStamina() <= LOW_STAMINA ? DANGER : MONEY_INK);
        }

        r.fillRect(30, 40, 420, 26, INK);
        r.drawText(40, 57, activePuzzle.getSnippet(), GOLD);

        List<String> options = activePuzzle.getOptions();
        for (int i = 0; i < options.size(); i++) {
            int ry = 82 + i * 22;
            r.fillRect(30, ry - 12, 420, 18, i == puzzleCursor ? GOLD : BORDER);
            r.fillRect(32, ry - 10, 416, 14, i == puzzleCursor ? CARD : PAPER);
            r.drawText(40, ry, (char) ('A' + i) + ".  " + options.get(i), INK);
        }
        String text = message.isEmpty() ? dialogueText : message;
        renderDialogueBox(r, wrap(text, WRAP_CHARS), "Z: submit   X: leave");
    }

    private void renderInventory(Renderer r) {
        r.drawPanel(20, 12, 440, 182, PAPER, BORDER);
        r.drawText(30, 30, "INVENTORY  " + getPlayer().getInventory().size() + "/"
                + getPlayer().getInventory().getCapacity(), INK);

        ItemCategory[] tabs = ItemCategory.values();
        for (int i = 0; i < tabs.length; i++) {
            int tx = 30 + i * 100;
            r.fillRect(tx, 38, 94, 18, tabs[i] == inventoryTab ? GOLD : BORDER);
            r.fillRect(tx + 2, 40, 90, 14, tabs[i] == inventoryTab ? CARD : PAPER);
            r.drawText(tx + 8, 51, tabs[i].getLabel(), INK);
        }

        List<Item> entries = inventoryEntries();
        if (entries.isEmpty()) {
            r.drawText(34, 82, "Nothing here yet.", INK);
        }
        for (int i = 0; i < entries.size() && i < 8; i++) {
            Item item = entries.get(i);
            int count = getPlayer().getInventory().countOf(item);
            int ry = 82 + i * 15;
            if (i == inventoryCursor) {
                r.fillRect(28, ry - 11, 424, 14, GOLD);
            }
            r.drawText(34, ry, item.getName(), INK);
            r.drawText(380, ry, "x" + count, MONEY_INK);
        }
        String text = message.isEmpty() ? "Choose an item." : message;
        renderDialogueBox(r, wrap(text, WRAP_CHARS), "Z: use   </>: tab   X/I: close");
    }

    static void renderMessageBox(Renderer r, String text) {
        r.drawPanel(6, SCREEN_HEIGHT - 30, SCREEN_WIDTH - 12, 24, PAPER, BORDER);
        r.drawText(14, SCREEN_HEIGHT - 14, text, INK);
    }

    /** Classic RPG dialogue box in the bottom third of the screen. */
    static void renderDialogueBox(Renderer r, List<String> lines, String hint) {
        int top = SCREEN_HEIGHT - 70;
        r.drawPanel(6, top, SCREEN_WIDTH - 12, 64, PAPER, BORDER);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            r.drawText(16, top + 18 + i * 12, lines.get(i), INK);
        }
        r.drawText(16, top + 58, hint, BORDER);
    }

    /** Splits text into lines of at most maxChars, breaking on spaces (a long word stays whole). */
    static List<String> wrap(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxChars) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}
