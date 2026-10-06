package isla.game;

import isla.InteractionResult;
import isla.Item;
import isla.ItemCategory;
import isla.NPC;
import isla.Player;
import isla.PurchaseResult;
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

    public Shop getActiveShop() {
        return activeShop;
    }

    public int getShopCursor() {
        return shopCursor;
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
        if (result.getKind() == InteractionResult.Kind.SHOP) {
            activeShop = result.getShop();
            shopCursor = 0;
            screen = Screen.SHOP;
        } else {
            screen = Screen.DIALOGUE;
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
        }
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
