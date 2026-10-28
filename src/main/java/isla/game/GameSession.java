package isla.game;

import isla.DebugQuest;
import isla.InteractionResult;
import isla.Item;
import isla.ItemCategory;
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
    static final String ENDING_STORY = "Elder Kai holds the Golden Idol up to the light and smiles. "
            + "\"The island remembers its treasures again,\" he says. Thanks to you, Isla is at peace.";

    static final String INK = "#3A2A12";
    static final String PAPER = "#F6EBC8";
    static final String CARD = "#FFF8E1";
    static final String BORDER = "#8A6A3A";
    static final String GOLD = "#FFD35A";
    static final String MONEY_INK = "#9A6B00";
    private static final int WRAP_CHARS = 76;
    static final int REST_AMOUNT = 10;

    private final GameWorld world;
    private Screen screen = Screen.EXPLORING;
    private String message = "";
    private String dialogueText = "";
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
        for (NPC npc : world.getNpcs()) {
            if (npc instanceof QuestGiver giver) {
                for (Quest q : giver.getQuests()) {
                    total++;
                    if (q.isCompleted()) {
                        done++;
                        earned += q.getReward();
                    }
                }
            }
        }
        return List.of("Quests completed: " + done + " of " + total,
                "Money earned from quests: $" + earned,
                "Money in hand: $" + getPlayer().getMoney());
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
        if (world.movePlayer(dx, dy) == MoveResult.TOO_TIRED) {
            message = "You are too tired to move. Press R to rest or use a consumable.";
        }
    }

    private void interact() {
        NPC npc = world.interactableNear().orElse(null);
        if (npc == null) {
            return;
        }
        InteractionResult result = npc.interact(getPlayer());
        dialogueText = result.getText();
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

    private void handleDialogue(Action action) {
        if (action == Action.CONFIRM || action == Action.CANCEL) {
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

    private void submitPuzzleAnswer() {
        DebugQuest puzzle = activePuzzle;
        if (puzzle.answer(getPlayer(), puzzleCursor)) {
            dialogueText = "That fixed it! Here is $" + puzzle.getReward() + " for your trouble.";
            closePuzzle();
            screen = Screen.DIALOGUE;
        } else {
            message = "Not quite. Read the line again and try another fix.";
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

    private void rest() {
        Player p = getPlayer();
        if (p.getStamina() >= p.getMaxStamina()) {
            message = "You are not tired.";
            return;
        }
        p.restoreStamina(REST_AMOUNT);
        message = "You catch your breath.";
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
        }
        switch (screen) {
            case EXPLORING -> {
                if (!message.isEmpty()) {
                    renderMessageBox(r, message);
                }
            }
            case DIALOGUE -> renderDialogueBox(r, wrap(dialogueText, WRAP_CHARS), "Z: close");
            case SHOP -> renderShop(r);
            case INVENTORY -> renderInventory(r);
            case DEBUG_PUZZLE -> renderPuzzle(r);
            case QUESTS -> renderQuestList(r);
            case ENDING -> renderEnding(r);
        }
    }

    private void renderQuestList(Renderer r) {
        r.fillRect(20, 12, 440, 112, BORDER);
        r.fillRect(22, 14, 436, 108, PAPER);
        r.drawText(30, 32, "QUESTS", INK);
        List<String> lines = questLines();
        for (int i = 0; i < lines.size(); i++) {
            String label = i == 0 ? "Goal: " : "Side: ";
            r.drawText(30, 56 + i * 18, label + lines.get(i), INK);
        }
        r.drawText(30, 112, "Q or X: close", BORDER);
    }

    private void renderEnding(Renderer r) {
        r.fillRect(20, 12, 440, 248, BORDER);
        r.fillRect(22, 14, 436, 244, PAPER);
        r.drawText(214, 36, "THE END", INK);
        List<String> story = wrap(ENDING_STORY, 66);
        for (int i = 0; i < story.size(); i++) {
            r.drawText(34, 62 + i * 14, story.get(i), INK);
        }
        r.drawText(34, 132, "Your story", MONEY_INK);
        List<String> summary = endingSummary();
        for (int i = 0; i < summary.size(); i++) {
            r.drawText(46, 152 + i * 16, summary.get(i), INK);
        }
        r.drawText(34, 224, "Z: keep exploring", INK);
        r.drawText(34, 242, "X: back to the main menu (saves first)", INK);
    }

    private void renderHud(Renderer r) {
        Player p = getPlayer();
        r.fillRect(4, 4, 120, 44, "#1E2A38");
        r.drawText(10, 17, "$" + p.getMoney(), GOLD);
        r.drawText(10, 29, "STA " + p.getStamina() + "/" + p.getMaxStamina(), "#FFFFFF");
        r.fillRect(10, 34, 108, 8, "#3A3A3A");
        int filled = p.getMaxStamina() == 0 ? 0 : 108 * p.getStamina() / p.getMaxStamina();
        r.fillRect(10, 34, filled, 8, "#5BD16B");
    }

    private void renderShop(Renderer r) {
        r.fillRect(20, 12, 440, 182, BORDER);
        r.fillRect(22, 14, 436, 178, PAPER);
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
        r.fillRect(20, 12, 440, 182, BORDER);
        r.fillRect(22, 14, 436, 178, PAPER);
        r.drawText(30, 30, "DEBUG: pick the fix", INK);
        r.drawText(396, 30, "$" + activePuzzle.getReward(), MONEY_INK);

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
        r.fillRect(20, 12, 440, 182, BORDER);
        r.fillRect(22, 14, 436, 178, PAPER);
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
        r.fillRect(6, SCREEN_HEIGHT - 30, SCREEN_WIDTH - 12, 24, BORDER);
        r.fillRect(8, SCREEN_HEIGHT - 28, SCREEN_WIDTH - 16, 20, PAPER);
        r.drawText(14, SCREEN_HEIGHT - 14, text, INK);
    }

    /** Classic RPG dialogue box in the bottom third of the screen. */
    static void renderDialogueBox(Renderer r, List<String> lines, String hint) {
        int top = SCREEN_HEIGHT - 70;
        r.fillRect(6, top, SCREEN_WIDTH - 12, 64, BORDER);
        r.fillRect(8, top + 2, SCREEN_WIDTH - 16, 60, PAPER);
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
