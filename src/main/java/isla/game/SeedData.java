package isla.game;

import isla.Consumable;
import isla.DebugQuest;
import isla.Item;
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
                new Souvenir(7, "Golden Idol", 60));
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

        return List.of(
                new Vendor(1, "Mara", 12, 12, "mara_sprite",
                        "Fresh fruit! Fill up before you wander far.", fruit),
                new Vendor(2, "Tomas", 28, 12, "tomas_sprite",
                        "Tools and trinkets, fair prices.", tools),
                new Vendor(3, "Yara", 30, 7, "trader_sprite",
                        "You found the jungle! Rare things for sale.", jungle));
    }

    public static Quest elderQuest() {
        return new Quest(1, 1, "Bring a coconut to Elder Kai", 25, "Coconut");
    }

    public static QuestGiver elder() {
        return elder(elderQuest());
    }

    /** Elder Kai's second wish: the Golden Idol (sold by Yara in the jungle). Quest row 3, an ordinary Quest. */
    public static Quest idolQuest() {
        return new Quest(3, 1, "Bring the Golden Idol to Elder Kai", 100, "Golden Idol");
    }

    /**
     * Elder Kai as the game uses him: the coconut quest, then the Golden Idol, then thanks. Delivering the Idol
     * ends the story. (elder() and elder(Quest) are the coconut-only Elder.)
     */
    public static QuestGiver elderWithIdol(Quest coconut, Quest idol) {
        return new QuestGiver(1, 20, 9, "elder_sprite", List.of(
                new QuestGiver.Stage(coconut,
                        "Could you bring me a coconut? My legs are not what they were.",
                        "Bring Elder Kai a coconut"),
                new QuestGiver.Stage(idol,
                        "That coconut was lovely! One last wish: a Golden Idol. Yara sells one deep in the jungle.",
                        "Find the Golden Idol in the jungle")),
                "Thank you, traveler. The island is at peace because of you.",
                "Done: Elder Kai has his Golden Idol").endsTheGame();
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
        return new QuestGiver(2, 24, 14, "coder_sprite", List.of(new QuestGiver.Stage(quest,
                "My code is broken and I cannot see why. Can you debug it?",
                "Help Coder Nia fix her code")), null, "Done: Coder Nia's code is fixed");
    }
}
