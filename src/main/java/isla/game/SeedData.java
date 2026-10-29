package isla.game;

import isla.Accusation;
import isla.Consumable;
import isla.DebugQuest;
import isla.GatherQuest;
import isla.Item;
import isla.Journey;
import isla.Quest;
import isla.QuestGiver;
import isla.QuestGiver.Stage;
import isla.Shop;
import isla.Souvenir;
import isla.Tool;
import isla.Vendor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The island's starting content and its story, "The Broken Source".
 *
 * <p>Isla runs on code: the Source, carved into the Golden Idol. On the night of a storm someone cut the Idol into
 * three shards (Sun, Moon, Sea) and the island began to glitch: the east bridge stopped drawing, the village got
 * stuck in a loop, the south jungle grows forever. The player fixes the cursed code stones on all three islands,
 * which opens the way, returns the shards and turns up clues. Back at Elder Kai they name the culprit and mend
 * the Idol together.
 *
 * <p>Every call builds fresh objects (quests and shops are mutable). The database is seeded from this class and
 * only remembers what changed (quest completed, stock left, flags), so all the story text lives here.
 */
public final class SeedData {

    public static final int PLAYER_ID = 1;
    public static final String PLAYER_NAME = "Traveler";
    public static final int NEW_GAME_MONEY = 50;
    public static final int MAX_STAMINA = 100;
    public static final int INVENTORY_CAPACITY = 6;
    public static final int SPAWN_X = 20;
    public static final int SPAWN_Y = 13;

    // ---- story flags (saved in the player's unlocked areas) ----
    public static final String JUNGLE_PATH = "Jungle Path";
    public static final String DARK_CANOPY = "Dark Canopy";
    public static final String EAST_BRIDGE = "East Bridge";
    public static final String SOUTH_BRIDGE = "South Bridge";
    public static final String OVERGROWTH = "Overgrowth Cleared";
    public static final String VILLAGE_AWAKE = "Village Awake";
    public static final String KNOWS_PASSWORD = "knows:password";
    public static final String SHARD_SUN = "shard:sun";
    public static final String SHARD_MOON = "shard:moon";
    public static final String SHARD_SEA = "shard:sea";
    public static final String CLUE_SIGNED = "clue:signed";
    public static final String CLUE_LIGHT = "clue:light";
    public static final String CLUE_MACHETE = "clue:machete";
    public static final String CLUE_LENSES = "clue:lenses";
    public static final String CLUE_SPARE_GLASSES = "clue:spare-glasses";

    /** The three shards, in the order they are usually found, with the name shown in the journal. */
    public static final Map<String, String> SHARDS = orderedMap(
            SHARD_SUN, "Sun", SHARD_MOON, "Moon", SHARD_SEA, "Sea");

    /** Every clue, with its journal line (at most 56 characters so it fits the journal). */
    public static final Map<String, String> CLUES = orderedMap(
            CLUE_SIGNED, "The shrine's broken code was signed '-N'.",
            CLUE_LIGHT, "Lio saw a square of blue light near the shrine.",
            CLUE_MACHETE, "Ula heard a machete chopping in the jungle.",
            CLUE_LENSES, "Two small round lenses lay cracked at the altar.",
            CLUE_SPARE_GLASSES, "Nia is wearing her spare glasses.");

    // ---- quest ids (the quests table) ----
    public static final int NIA_PRACTICE_ID = 2; // drawn from DebugQuestBank, see GameBootstrap
    public static final int KAI_INTRO_ID = 10;
    public static final int KAI_SHARDS_ID = 11;
    public static final int ACCUSATION_ID = 12;
    public static final int MEND_IDOL_ID = 13;
    public static final int LIO_ID = 20;
    public static final int ULA_ID = 21;
    public static final int PIP_ID = 22;
    public static final int SHRINE_ID = 30;
    public static final int VILLAGE_STONE_ID = 31;
    public static final int SOUTH_STONE_ID = 32;
    public static final int VINE_STONE_ID = 33;
    public static final int ALTAR_ID = 34;
    public static final int NIA_CHAT_ID = 35;

    /** Who it was. Kept here so tests and the ending agree. */
    public static final String CULPRIT = "Coder Nia";

    private SeedData() {
    }

    @SafeVarargs
    private static <T> Map<T, T> orderedMap(T... keyValues) {
        Map<T, T> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    // =====================================================================================
    // Items and shops
    // =====================================================================================

    public static List<Item> items() {
        return List.of(
                new Consumable(1, "Coconut Water", 10, 20),
                new Consumable(2, "Coconut", 8, 10),
                new Consumable(3, "Banana", 5, 8),
                new Tool(4, "Rusty Machete", 30, JUNGLE_PATH),
                new Souvenir(5, "Seashell Necklace", 15),
                new Souvenir(6, "Carved Tiki", 25),
                new Souvenir(7, "Golden Idol", 60), // kept in the catalog so very old saves still load
                new Consumable(8, "Grilled Fish", 12, 40),
                new Tool(9, "Lantern", 35, DARK_CANOPY),
                new Consumable(10, "Stamina Tonic", 25, 100));
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
        jungle.addStock(item(c, 10), 2);
        jungle.addStock(item(c, 6), 1);

        Shop village = new Shop();
        village.addStock(item(c, 8), 5);
        village.addStock(item(c, 9), 1);
        village.addStock(item(c, 5), 1);

        return List.of(
                new Vendor(1, "Mara", 12, 12, "mara_sprite",
                        "Fresh fruit! The roads feel longer since the Idol broke. Eat before you wander.", fruit),
                new Vendor(2, "Tomas", 28, 12, "tomas_sprite",
                        "Tools and trinkets. Someone borrowed my old machete the night of the storm... never mind.",
                        tools),
                new Vendor(3, "Yara", 30, 7, "trader_sprite",
                        "Shh. Kai doesn't like me trading this close to the shrine. Rare things for sale.", jungle),
                new Vendor(4, "Ofa", 67, 15, "ofa_sprite",
                        "Lovely da... oh! A customer! I slept through the loop in my hut. Hot fish, and a lantern"
                                + " for dark places.", village));
    }

    // =====================================================================================
    // Elder Kai: the main story
    // =====================================================================================

    public static Quest kaiIntroQuest() {
        return new Quest(KAI_INTRO_ID, 1, "Hear Elder Kai out", 20, null);
    }

    public static GatherQuest shardsQuest() {
        return new GatherQuest(KAI_SHARDS_ID, 1, "Bring Elder Kai the three Idol shards", 50,
                List.copyOf(SHARDS.keySet()));
    }

    public static Accusation accusation() {
        Accusation a = new Accusation(ACCUSATION_ID, 1, "Name who broke the Golden Idol", 0,
                "Who cut the Golden Idol on the night of the storm?",
                List.of("Tomas, the toolmaker", "Yara, the jungle trader", CULPRIT, "Elder Kai himself"), 2);
        a.withFeedback(Arrays.asList(
                "Tomas folds his arms: 'I can't read a line of code, and I'd never sign my work N.'",
                "Yara laughs: 'I trade, I don't code. And I've never owned a glowing screen.'",
                null,
                "Kai sighs: 'My old eyes cannot read the stones, child. Think about those lenses.'"));
        return a;
    }

    public static DebugQuest mendIdolQuest() {
        DebugQuest q = new DebugQuest(MEND_IDOL_ID, 1, "Mend the Golden Idol", 0,
                "idol.attach(sun); idol.attach(moon); idol.attach(sun);",
                List.of("Attach moon twice instead", "Make the last line idol.attach(sea);",
                        "Remove the semicolons", "Swap sun and moon"), 1);
        q.withFeedback(Arrays.asList(
                "Now moon is doubled. Count the shards: sun, moon and sea, once each.",
                null,
                "Every Java statement needs its semicolon. That would not even compile.",
                "The order is not the problem: one shard is attached twice and one is missing."));
        return q;
    }

    /** Elder Kai with the four quests of the main story; the last one ends the game. */
    public static QuestGiver elder(Quest intro, Quest shards, Quest accusation, Quest mend) {
        return new QuestGiver(1, 20, 9, "elder_sprite", List.of(
                new Stage(intro, "Ah, a traveler.", "Talk to Elder Kai, north of the road", null, null,
                        "Traveler, you came in a dark week. Three nights ago, during the storm, someone cut"
                                + " our Golden Idol into three shards. The Idol holds the Source: the code our"
                                + " island runs on. Since then Isla glitches. The east bridge will not draw, the"
                                + " village is stuck repeating itself, and the south jungle grows without end."
                                + " Fix the cursed stones, bring back the Sun, Moon and Sea shards, and find out"
                                + " who did this. Take $20 for supplies. Coder Nia, just south of the road, can"
                                + " teach you to read code. The Jungle Shrine is in the trees to the north-east;"
                                + " Tomas sells a machete to get through."),
                new Stage(shards, "Bring me the Sun, Moon and Sea shards. Press Q to see your journal.",
                        "Bring the three shards back to Elder Kai", null, null,
                        "Kai fits the three shards together. They glow, but they will not join. 'The Source"
                                + " remembers who broke it,' he says, 'and it will not heal until the truth is"
                                + " spoken. Read your clues (Q), then tell me: who cut the Idol?'"),
                new Stage(accusation, "Read your clues (Q). Who cut the Golden Idol?",
                        "Tell Elder Kai who broke the Idol", null, null,
                        "Nia comes running up the path, her spare glasses crooked. 'It was me,' she says."
                                + " 'I wanted to add a welcome feature, so the bridges would greet visitors. I"
                                + " cut through the jungle with Tomas's old machete and patched the Source by"
                                + " laptop light, in a hurry, and it split. I hid the shards and blamed the"
                                + " storm. I was so ashamed.' Kai puts a hand on her shoulder. 'Then we mend it"
                                + " together. Traveler, read the Source with us.'"),
                new Stage(mend, "The Idol's Source glows in the air. One line is still wrong.",
                        "Mend the Golden Idol with Elder Kai", null, null,
                        "The three shards lock together with a sound like a bell.")),
                "The Idol shines again, and so does Isla. Thank you, traveler.",
                "Done: the Golden Idol is whole again").endsTheGame();
    }

    // =====================================================================================
    // Coder Nia: the debugging teacher (and, it turns out, the culprit)
    // =====================================================================================

    /** A fixed practice puzzle for tests and as a fallback; a real game draws one from DebugQuestBank. */
    public static DebugQuest debugQuest() {
        return new DebugQuest(NIA_PRACTICE_ID, 2, "Fix Nia's broken stamina check", 40,
                "if (stamina = 0) { rest(); }",
                List.of("Change = to !=",
                        "Change = to ==",
                        "Remove the semicolon",
                        "Rename stamina to energy"),
                1);
    }

    public static Quest niaChatQuest() {
        return new Quest(NIA_CHAT_ID, 2, "Ask Nia about her glasses", 0, null).grants(CLUE_SPARE_GLASSES);
    }

    public static QuestGiver coder() {
        return coder(debugQuest(), niaChatQuest());
    }

    /** Nia with a given practice puzzle and the small chat that holds a clue. */
    public static QuestGiver coder(Quest practice, Quest chat) {
        return new QuestGiver(2, 24, 14, "coder_sprite", List.of(
                new Stage(practice,
                        "Hi, I'm Nia! The stones all run on code, and code breaks. Try a practice bug from my"
                                + " laptop. Careful: a wrong fix gives you a little shock.",
                        "Learn debugging from Coder Nia", null, null,
                        "You're a natural! The real stones are harder. The"
                                + " Jungle Shrine is in the trees to the north-east. I... don't go near the"
                                + " shrines myself."),
                new Stage(chat, "", "Chat with Coder Nia", null, null,
                        "Hm? My glasses? Oh, these are my spare pair. The old ones... broke. Anyway! Good luck"
                                + " out there.")),
                "Go on, the stones won't fix themselves! ...Sorry. I'm a bit jumpy lately.",
                "Done: Nia taught you to debug");
    }

    // =====================================================================================
    // Code stones: the curse made visible. Each fix opens the way and turns up a shard or a clue.
    // =====================================================================================

    public static DebugQuest shrinePuzzle() {
        DebugQuest q = new DebugQuest(SHRINE_ID, 7, "Fix the Jungle Shrine stone", 30,
                "for (int p = 0; p > length; p++) placePlank(p);  // -N",
                List.of("Change p > length to p < length", "Change p++ to p--", "Start the loop at p = 1",
                        "Delete the // -N comment"), 0);
        q.withFeedback(Arrays.asList(null,
                "p-- would count away from length, and the loop still never starts: 0 > length is false.",
                "Starting at 1 skips the first plank, and 1 > length is still false.",
                "Comments never run, so deleting it changes nothing. But who is N?"));
        q.grants(EAST_BRIDGE, SHARD_SUN, CLUE_SIGNED);
        return q;
    }

    public static DebugQuest villagePuzzle() {
        DebugQuest q = new DebugQuest(VILLAGE_STONE_ID, 8, "Fix the village stone", 30,
                "while (hour < 24) { say(\"Lovely day!\"); }",
                List.of("Change < to <=", "Add hour++; inside the loop", "Change 24 to 12",
                        "Print hour instead of the greeting"), 1);
        q.withFeedback(Arrays.asList(
                "Now it loops while hour <= 24, and hour still never changes!",
                null,
                "A smaller limit does not help if hour never moves.",
                "Then they would shout the hour forever. Something must change hour."));
        q.grants(VILLAGE_AWAKE, SHARD_MOON);
        return q;
    }

    public static DebugQuest southPuzzle() {
        DebugQuest q = new DebugQuest(SOUTH_STONE_ID, 9, "Fix the South Bridge stone", 20,
                "if (word.equals(\"ULA\")) openBridge();  // word is \"ula\"",
                List.of("Use word == \"ULA\"", "Use word = \"ULA\"", "Use word.equalsIgnoreCase(\"ULA\")",
                        "Change \"ULA\" to ULA"), 2);
        q.withFeedback(Arrays.asList(
                "== compares two objects, not their letters, and the case still differs.",
                "= assigns instead of comparing. That would not even compile here.",
                null,
                "Without quotes ULA is a variable, and there is no variable called ULA."));
        q.grants(SOUTH_BRIDGE);
        return q;
    }

    public static DebugQuest vinePuzzle() {
        DebugQuest q = new DebugQuest(VINE_STONE_ID, 10, "Fix the Overgrowth stone", 20,
                "void grow(int n) { plant(n); grow(n + 1); }",
                List.of("Change n + 1 to n - 1", "Make grow static",
                        "Stop first: if (n > 9) return;", "Rename n to size"), 2);
        q.withFeedback(Arrays.asList(
                "Counting down never stops either. A recursive method needs a base case.",
                "static changes who owns the method, not when it stops calling itself.",
                null,
                "A new name, the same endless recursion."));
        q.grants(OVERGROWTH);
        return q;
    }

    public static DebugQuest altarPuzzle() {
        DebugQuest q = new DebugQuest(ALTAR_ID, 11, "Fix the temple altar", 40,
                "int percent = found / 3 * 100;   // found = 2, shows 0",
                List.of("Write found * 100 / 3", "Remove the * 100", "Change int to String",
                        "Change 3 to 3.0"), 0);
        q.withFeedback(Arrays.asList(null,
                "2 / 3 is still 0 in whole-number maths.",
                "Text cannot hold a number you can do maths with.",
                "2 / 3.0 * 100 is a double (66.6...), and it will not fit in an int without a cast."));
        q.grants(SHARD_SEA, CLUE_LENSES);
        return q;
    }

    public static QuestGiver shrineStone(Quest q) {
        return stone(7, 28, 5, q,
                "An old stone tablet deep in the jungle, its glyphs flickering. This is the code that draws the"
                        + " east bridge, and it never places a single plank.",
                "Get a machete, fix the Jungle Shrine stone", null, null,
                "The glyphs settle. Far to the east, planks shimmer into place across the water! A warm SUN"
                        + " SHARD rises out of the stone. The last line was signed '-N'. (Clue noted: press Q.)",
                "The shrine is quiet. The east bridge holds.");
    }

    public static QuestGiver villageStone(Quest q) {
        return stone(8, 60, 12, q,
                "The village stone hums the same note over and over. Its code is stuck in a loop, and so is"
                        + " everyone around it.",
                "Cross the east bridge, fix the village stone", null, null,
                "Time lurches forward. All around you villagers blink, stretch and look about in confusion. A"
                        + " cool silver MOON SHARD drops into your hands. Talk to the villagers!",
                "The village stone ticks along, one hour at a time.");
    }

    public static QuestGiver southStone(Quest q) {
        return stone(9, 22, 23, q,
                "You whisper 'ula'. The stone still refuses! Its code compares the word the wrong way.",
                "Unlock the South Bridge stone with Ula's word", KNOWS_PASSWORD,
                "The stone flickers: ENTER PASSWORD. You don't know it. Whoever set it must live in the village"
                        + " across the east bridge.",
                "The stone accepts the word. To the south, a bridge draws itself plank by plank over the sea. The"
                        + " big island waits.",
                "The south bridge stands firm.");
    }

    public static QuestGiver vineStone(Quest q) {
        return stone(10, 50, 41, q,
                "By lantern light the glyphs appear. The code that grows the jungle calls itself forever.",
                "Light a lantern, fix the stone by the red vines", DARK_CANOPY,
                "Twisted red vines block the way east, and the stone under them is too dark to read. A lantern"
                        + " would help: the village sells one. (Use it from your inventory.)",
                "The vines shudder and wither into plain grass. In the clearing beyond stands an old temple"
                        + " altar.",
                "Just grass and quiet now.");
    }

    public static QuestGiver altar(Quest q) {
        return stone(11, 55, 44, q,
                "The temple altar shows the Idol's repair counter. You hold two shards, but it says 0%.",
                "Fix the temple altar beyond the vines", null, null,
                "The counter jumps to 66%. A deep blue SEA SHARD slides out of the altar. Beside it lie two"
                        + " small round lenses, cracked, as if from a pair of glasses. (Clue noted.)",
                "The altar waits for the Idol to come home.");
    }

    private static QuestGiver stone(int npcId, int x, int y, Quest q, String dialogue, String goal,
                                    String requires, String locked, String done, String after) {
        return new QuestGiver(npcId, x, y, "stone_sprite",
                List.of(new Stage(q, dialogue, goal, requires, locked, done)), after, "Done: " + goal);
    }

    // =====================================================================================
    // The village: frozen in a loop until its stone is fixed
    // =====================================================================================

    public static Quest lioQuest() {
        return new Quest(LIO_ID, 5, "Talk to Fisher Lio", 0, null).grants(CLUE_LIGHT);
    }

    public static Quest ulaQuest() {
        return new Quest(ULA_ID, 6, "Bring Grandma Ula a Seashell Necklace", 10, "Seashell Necklace")
                .grants(KNOWS_PASSWORD, CLUE_MACHETE);
    }

    public static Quest pipQuest() {
        return new Quest(PIP_ID, 12, "Talk to Pip", 5, null);
    }

    private static final String LOOP = "Lovely day! Lovely day! Lovely day! Lovely day! Lovely day! Lovely day!"
            + " Lovely day! Lovely da...";

    public static QuestGiver lio(Quest q) {
        return new QuestGiver(5, 75, 13, "lio_sprite", List.of(new Stage(q, "", "Talk to Fisher Lio",
                VILLAGE_AWAKE, LOOP,
                "Whoa, have I been saying that for days? Thanks, friend. Listen: the night the Idol cracked I was"
                        + " out on my boat. I saw a little square of blue light bobbing up the jungle path toward"
                        + " the shrine. Like a screen.")),
                "The fish are biting again! Good luck with those shards.", "Done: Lio told you what he saw");
    }

    public static QuestGiver ula(Quest q) {
        return new QuestGiver(6, 56, 12, "ula_sprite", List.of(new Stage(q,
                "Oh my, how many days have I lost? The south bridge stone? I set its password myself, years ago."
                        + " But my memory works better with my seashell necklace, and the storm took it. Bring"
                        + " me one, dear. Tomas or Ofa sell them.",
                "Bring Grandma Ula a Seashell Necklace", VILLAGE_AWAKE, LOOP,
                "Lovely, just like my old one! The password is my name, all small letters: 'ula'. The stone is"
                        + " fussy about that. And dear... that night I heard a machete chopping through the"
                        + " jungle. Who chops trees at midnight?")),
                "The south bridge stone is at the bottom of your home island, dear.",
                "Done: Ula told you the password");
    }

    public static QuestGiver pip(Quest q) {
        return new QuestGiver(12, 60, 19, "pip_sprite", List.of(new Stage(q, "", "Talk to Pip",
                VILLAGE_AWAKE, "Tag! You're it! Tag! You're it! Tag! You're it! Tag! You're it! Tag!",
                "Huh? Was I playing tag for three days? Grandma Ula knows everything about the old stones, ask"
                        + " her! Here, I found $5 in the sand.")),
                "Tag! ...just kidding.", "Done: Pip is playing again");
    }

    // =====================================================================================
    // The whole story, in order: what the objective strip and journal show.
    // =====================================================================================

    /**
     * The main story as chapters, using the very quest objects the NPCs hand out (so completing a quest moves the
     * journey on). Optional side talks (Nia's practice, Lio, Pip) are not chapters.
     */
    public static Journey journey(Quest intro, Quest shrine, Quest village, Quest ula, Quest south, Quest vines,
                                  Quest altar, Quest shards, Quest accusation, Quest mend) {
        String one = "Chapter 1: The Missing Bridge";
        String two = "Chapter 2: The Looping Village";
        String three = "Chapter 3: The Endless Jungle";
        String finale = "Finale: The Truth";
        return new Journey("The Broken Source", "The Idol is whole. Explore Isla freely!", List.of(
                new Journey.Chapter(intro, "Talk to Elder Kai, north of the road", one),
                new Journey.Chapter(shrine, "Get a machete, fix the Jungle Shrine stone (NE)", one),
                new Journey.Chapter(village, "Cross the east bridge, fix the village stone", two),
                new Journey.Chapter(ula, "Help Grandma Ula remember the password", two),
                new Journey.Chapter(south, "Unlock the South Bridge stone (south shore)", three),
                new Journey.Chapter(vines, "Light a lantern, fix the stone by the red vines", three),
                new Journey.Chapter(altar, "Fix the temple altar beyond the vines", three),
                new Journey.Chapter(shards, "Bring the three shards back to Elder Kai", finale),
                new Journey.Chapter(accusation, "Tell Elder Kai who broke the Idol", finale),
                new Journey.Chapter(mend, "Mend the Golden Idol with Elder Kai", finale)));
    }
}
