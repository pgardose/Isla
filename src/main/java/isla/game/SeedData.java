package isla.game;

import isla.Consumable;
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
                new Vendor(1, "Mara", 12, 12, "vendor_sprite",
                        "Fresh fruit! Fill up before you wander far.", fruit),
                new Vendor(2, "Tomas", 28, 12, "vendor_sprite",
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

    public static QuestGiver elder(Quest quest) {
        return new QuestGiver(1, 20, 9, "elder_sprite",
                "Could you bring me a coconut? My legs are not what they were.", quest);
    }
}
