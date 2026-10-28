package isla.game;

import isla.Consumable;
import isla.DebugQuest;
import isla.Item;
import isla.Journey;
import isla.Quest;
import isla.QuestGiver;
import isla.Shop;
import isla.Souvenir;
import isla.Tool;
import isla.Vendor;
import java.util.List;

/**
 * The island's starting content: item catalog, vendors with their stock, and
 * the elder's fetch quest. Every call builds fresh objects (shops are
 * mutable). The database is seeded from this class, so there is one source of
 * truth for the starting world.
 */
public final class SeedData {

    public static final int PLAYER_ID = 1;
    public static final String PLAYER_NAME = "Traveler";
    public static final int NEW_GAME_MONEY = 50;
    public static final int MAX_STAMINA = 100;
    public static final int INVENTORY_CAPACITY = 6;
    public static final int SPAWN_X = 20;
    public static final int SPAWN_Y = 13;

    private SeedData() {
    }

    public static List<Item> items() {
        return List.of(
                new Consumable(1, "Coconut Water", 10, 20),
                new Consumable(2, "Coconut", 8, 10),
                new Consumable(3, "Banana", 5, 8),
                new Tool(4, "Rusty Machete", 30, "Jungle Path"),
                new Souvenir(5, "Seashell Necklace", 15),
                new Souvenir(6, "Carved Tiki", 25),
                new Souvenir(7, "Golden Idol", 60),
                // Story items: bought from far-away vendors and handed in for the lighthouse quests.
                new Souvenir(8, "Lamp Oil", 20),
                new Souvenir(9, "Glass Lens", 45));
    }

    private static Item item(List<Item> catalog, int id) {
        return catalog.stream().filter(i -> i.getId() == id).findFirst().orElseThrow();
    }

    public static List<Vendor> vendors() {
        List<Item> c = items();

        Shop fruit = new Shop();
        fruit.addStock(item(c, 1), 5);
        fruit.addStock(item(c, 2), 3);
        fruit.addStock(item(c, 3), 5);

        Shop tools = new Shop();
        tools.addStock(item(c, 4), 1);
        tools.addStock(item(c, 5), 2);
        tools.addStock(item(c, 6), 2);

        Shop jungle = new Shop();
        jungle.addStock(item(c, 7), 1);
        jungle.addStock(item(c, 6), 1);

        Shop south = new Shop();
        south.addStock(item(c, 8), 2);
        south.addStock(item(c, 1), 3);

        Shop deepJungle = new Shop();
        deepJungle.addStock(item(c, 9), 2);

        return List.of(
                new Vendor(1, "Mara", 12, 12, "vendor_sprite",
                        "Fresh fruit! Fill up before you wander far.", fruit),
                new Vendor(2, "Tomas", 28, 12, "vendor_sprite",
                        "Tools and trinkets, fair prices.", tools),
                new Vendor(3, "Yara", 30, 7, "trader_sprite",
                        "You found the jungle! Rare things for sale.", jungle),
                new Vendor(4, "Dalia", 30, 36, "vendor_sprite",
                        "Lamp oil, straight from the south! The lighthouse keeper always runs out.", south),
                new Vendor(5, "Old Pearl", 28, 5, "trader_sprite",
                        "A glass lens? Only the finest, from deep in the jungle.", deepJungle));
    }

    public static Quest elderQuest() {
        return new Quest(1, 1, "Bring a coconut to Elder Kai", 25, "Coconut")
                .withCompletionText("Bless you! Now a bigger favor: the lighthouse in the east has gone dark "
                        + "and ships cannot find our harbor. Walk east along the path and speak to Keeper Bo. (+$25)");
    }

    public static QuestGiver elder() {
        return elder(elderQuest());
    }

    public static QuestGiver elder(Quest quest) {
        return new QuestGiver(1, 20, 9, "elder_sprite",
                "Could you bring me a coconut? My legs are not what they were.", quest);
    }

    /** Nia's debugging puzzle. The correct fix is deliberately not the first option. */
    public static DebugQuest debugQuest() {
        return new DebugQuest(2, 2, "Fix Nia's broken stamina check", 40,
                "if (stamina = 0) { rest(); }",
                List.of("Change = to !=",
                        "Change = to ==",
                        "Remove the semicolon",
                        "Rename stamina to energy"),
                1);
    }

    public static QuestGiver coder() {
        return coder(debugQuest());
    }

    public static QuestGiver coder(Quest quest) {
        return new QuestGiver(2, 24, 14, "coder_sprite",
                "My code is broken and I cannot see why. Can you debug it?", quest);
    }

    // ---- the main story: relight the lighthouse ----

    /** Chapter 2: Keeper Bo wants lamp oil (sold by Dalia on the south island). */
    public static Quest keeperQuest() {
        return new Quest(3, 3, "Bring Lamp Oil to Keeper Bo", 30, "Lamp Oil")
                .withCompletionText("Lamp oil at last! But the lamp's controller code crashes on start. "
                        + "Coder Nia in the west village can debug it. (+$30)");
    }

    public static QuestGiver keeper() {
        return keeper(keeperQuest());
    }

    public static QuestGiver keeper(Quest quest) {
        return new QuestGiver(3, 60, 8, "keeper_sprite",
                "The great lamp has been dark for weeks. Bring me lamp oil: Dalia sells it, far to the south.", quest)
                .withLockedDialogue("Hello! I keep this lighthouse. Elder Kai in the west village "
                        + "said he would send someone to help.");
    }

    /** Chapter 4 (the finale): fit the Glass Lens, which only Old Pearl in the jungle sells. */
    public static Quest lampQuest() {
        return new Quest(4, 4, "Fit the Glass Lens into the lamp", 100, "Glass Lens")
                .withCompletionText("The beam sweeps across the sea! You relit Isla's lighthouse and ships can "
                        + "find home again. THE END - thanks for playing! (+$100)");
    }

    public static QuestGiver lamp() {
        return lamp(lampQuest());
    }

    public static QuestGiver lamp(Quest quest) {
        return new QuestGiver(4, 64, 8, "lamp_sprite",
                "The great lamp stands cold. It needs a glass lens to focus the beam.", quest)
                .withLockedDialogue("The great lamp stands cold. Keeper Bo says it needs lamp oil "
                        + "and fixed code first.");
    }

    /**
     * Joins the four quests into one story and returns it. Elder Kai opens it, Keeper Bo needs the Elder done,
     * and the lamp needs both Bo and Nia's debugging done. Nia has no prerequisite, so the puzzle can be
     * played any time.
     */
    public static Journey story(Quest elder, Quest keeper, Quest debug, Quest lamp) {
        keeper.requires(elder);
        lamp.requires(keeper, debug);
        return new Journey("RELIGHT THE LIGHTHOUSE", "The lighthouse shines again! Thanks for playing.", List.of(
                new Journey.Chapter(elder, "Bring a coconut to Elder Kai (just north of spawn)"),
                new Journey.Chapter(keeper, "Buy Lamp Oil from Dalia (south) for Keeper Bo (east)"),
                new Journey.Chapter(debug, "Debug the lamp code with Coder Nia (near spawn)"),
                new Journey.Chapter(lamp, "Get a Glass Lens (jungle, needs Machete) for the lamp")));
    }
}
