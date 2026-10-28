package isla.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every sprite in the game, drawn in code so no image files are needed. Each frame is a String[16]:
 * sixteen rows of sixteen letters. A letter is a colour from {@link Palette} ('.' is see-through).
 * An artist can replace any of these later by dropping {@code sprites/<name>.png} into the resources
 * folder (see {@link SpriteLibrary}).
 */
public final class Sprites {

    public static final int SIZE = 16;

    private static final Map<String, List<String[]>> FRAMES = new LinkedHashMap<>();
    private static final Map<String, String> KEYS = new LinkedHashMap<>();

    // The player, from the team's character art: red cap with a tree badge, blonde hair, shades,
    // green jacket, black trousers. Frame 1 is the same character one pixel higher, mid-stride.
    private static final String[] PLAYER_0 = {
        "................",
        "....IIIIIIII....",
        "...ICCCCCCCCI...",
        "...ICAAGGAACI...",
        "...ICAAWWAACI...",
        "..ICCCCCCCCCCI..",
        "..IYSSSSSSSSYI..",
        "..IYIAISSIAIYI..",
        "..IYSSSCCSSSYI..",
        "...IYSSSSSSYI...",
        "..IPPPPAAPPPPI..",
        "..IPJPPAAPPJPI..",
        "..IJJJJAAJJJJI..",
        "....IIIIIIII....",
        "....IIII..IIII..",
        "....IIII..IIII..",
    };
    private static final String[] PLAYER_1 = {
        "....IIIIIIII....",
        "...ICCCCCCCCI...",
        "...ICAAGGAACI...",
        "...ICAAWWAACI...",
        "..ICCCCCCCCCCI..",
        "..IYSSSSSSSSYI..",
        "..IYIAISSIAIYI..",
        "..IYSSSCCSSSYI..",
        "...IYSSSSSSYI...",
        "..IPPPPAAPPPPI..",
        "..IPJPPAAPPJPI..",
        "..IJJJJAAJJJJI..",
        "....IIIIIIII....",
        "....III..IIII...",
        "....III....III..",
        "................",
    };

    // Mara: fruit seller. Straw hat, brown plaits, coral apron with golden fruit.
    private static final String[] MARA = {
        "................",
        ".....IIIIII.....",
        "....IssssssI....",
        "....IsYYYYsI....",
        "..IIssssssssII..",
        "..IWWSSSSSSWWI..",
        "..IWSISSSSISWI..",
        "..IWSSSCCSSSWI..",
        "...IWSSSSSSWI...",
        "..IAACCCCCCAAI..",
        "..IACCCCCCCCAI..",
        "..ICCYCCCCYCCI..",
        "..ICCCCCCCCCCI..",
        "...ICCCCCCCCI...",
        "....SS....SS....",
        "...IIII..IIII...",
    };

    // Tomas: tool seller. Coral headband, short black hair, green shirt, wooden work apron.
    private static final String[] TOMAS = {
        "................",
        "....IIIIIIII....",
        "...IIIIIIIIII...",
        "...ICCCCCCCCI...",
        "...IssssssssI...",
        "..IIsIssssIsII..",
        "...IsssWWsssI...",
        "....IssssssI....",
        "..IggggggggggI..",
        "..IgWWWWWWWWgI..",
        "..IgWAAWWDDWgI..",
        "..IgWWWWWWWWgI..",
        "..IgWWWWWWWWgI..",
        "...IIWWWWWWII...",
        "....IIII..IIII..",
        "....IIII..IIII..",
    };

    // Yara: jungle trader. Black hair, gold headband with a coral feather, leaf clothes.
    private static final String[] YARA = {
        "................",
        "....IIIIIIII....",
        "...IIIIIIIIIC...",
        "...IYYYYYYYYC...",
        "..IITTTTTTTTII..",
        "..IITITTTTITII..",
        "..IITTTCCTTTII..",
        "..IIITTTTTTIII..",
        "..IIYYYYYYYYII..",
        "..IIPPPPPPPPII..",
        "..IIPJPPPPJPII..",
        "...ITTTTTTTTI...",
        "..IJJJJJJJJJJI..",
        "..IJPJPJPJPJJI..",
        "....TT....TT....",
        "...IIII..IIII...",
    };

    // Elder Kai: white hair and beard, deep-blue robe with gold trim, tall staff with a golden top.
    private static final String[] ELDER_KAI = {
        "................",
        ".....FFFFFF.....",
        "YY..FFFFFFFF....",
        "YY.IFFFFFFFFI...",
        ".W.IFTTTTTTFI...",
        ".W.IFTITTITFI...",
        ".W.IFFTTTTFFI...",
        ".W..IFFFFFFI....",
        ".W...IFFFFI.....",
        ".W.IOOOOFFOOOOI.",
        ".W.IOOYOOOOYOOI.",
        ".W.IOOOOOOOOOOI.",
        ".W.IOOYYYYYYOOI.",
        ".W.IOOOOOOOOOOI.",
        ".W.IOOOOOOOOOOI.",
        ".W..IIIIIIIIII..",
    };

    // Coder Nia: coral hair bun, big round glasses, light-blue hoodie, a laptop held at her chest.
    private static final String[] CODER_NIA = {
        ".....CCCC.......",
        "....CCCCCCCC....",
        "...ICCCCCCCCI...",
        "...ICCSSSSCCI...",
        "...ISSSSSSSSI...",
        "..IIIFFIIFFIII..",
        "...ISSSSSSSSI...",
        "...ISSSCCSSSI...",
        "....ISSSSSSI....",
        "..IooooooooooI..",
        "..IooDDDDDDooI..",
        "..IooDFFFFDooI..",
        "..IooDDDDDDooI..",
        "..IooooooooooI..",
        "....DDDD..DDDD..",
        "...CCCC..CCCC...",
    };

    // A palm tree: fronds, a few coconuts and a curved trunk. Drawn over a grass tile.
    private static final String[] PALM_TREE = {
        ".....PPPPP......",
        "..PPPPPJPPPPP...",
        ".PPJPPPPPPPJPPP.",
        "PPJ..PPJPP..JPP.",
        "PJ....IWI....JP.",
        "P......WI.......",
        ".......WWI......",
        "......TWWI......",
        "......TWI.......",
        ".......WWI......",
        ".......WWI......",
        "......TWWI......",
        "......TWI.......",
        ".....TTWWI......",
        "....gTWWWIg.....",
        "...gg.gIg.gg....",
    };

    // A hut: thatched roof, wooden walls, dark doorway. Drawn over a grass tile.
    private static final String[] HUT_HOUSE = {
        "................",
        ".......ss.......",
        "......sYYs......",
        ".....sYYYYs.....",
        "....sYssYYYs....",
        "...sYYYYYYYYs...",
        "..sYssYYYssYYs..",
        ".IIIIIIIIIIIIII.",
        "..IWWWWWWWWWWI..",
        "..IWTWWTWWTWWI..",
        "..IWWWWIIWWWWI..",
        "..IWTWWIIWWTWI..",
        "..IWWWWIIWWWWI..",
        "..IWWWWIIWWWWI..",
        ".IIIIIIIIIIIIII.",
        "................",
    };

    // "!" marks a shop (gold bubble); "?" marks someone who needs help (coral bubble).
    private static final String[] EXCLAIM = {
        "................",
        "....IIIIIIII....",
        "...IYYYYYYYYI...",
        "...IYYYIIYYYI...",
        "...IYYYIIYYYI...",
        "...IYYYIIYYYI...",
        "...IYYYIIYYYI...",
        "...IYYYYYYYYI...",
        "...IYYYIIYYYI...",
        "...IYYYYYYYYI...",
        "....IIIIIIII....",
        ".......II.......",
        "................",
        "................",
        "................",
        "................",
    };
    private static final String[] QUESTION = {
        "................",
        "....IIIIIIII....",
        "...ICCCCCCCCI...",
        "...ICCAAAACCI...",
        "...ICCCCCAACI...",
        "...ICCCCAACCI...",
        "...ICCCAACCCI...",
        "...ICCCCCCCCI...",
        "...ICCCAACCCI...",
        "...ICCCCCCCCI...",
        "....IIIIIIII....",
        ".......II.......",
        "................",
        "................",
        "................",
        "................",
    };

    static {
        FRAMES.put("player", List.of(PLAYER_0, PLAYER_1));
        FRAMES.put("mara", List.<String[]>of(MARA));
        FRAMES.put("tomas", List.<String[]>of(TOMAS));
        FRAMES.put("yara", List.<String[]>of(YARA));
        FRAMES.put("elder_kai", List.<String[]>of(ELDER_KAI));
        FRAMES.put("coder_nia", List.<String[]>of(CODER_NIA));
        FRAMES.put("palm", List.<String[]>of(PALM_TREE));
        FRAMES.put("hut", List.<String[]>of(HUT_HOUSE));
        FRAMES.put("exclaim", List.<String[]>of(EXCLAIM));
        FRAMES.put("question", List.<String[]>of(QUESTION));

        // The sprite keys the game uses. "vendor_sprite" is what every vendor used before Mara and
        // Tomas got their own looks, so old saved games still show a sensible sprite.
        KEYS.put("player_sprite", "player");
        KEYS.put("mara_sprite", "mara");
        KEYS.put("tomas_sprite", "tomas");
        KEYS.put("vendor_sprite", "mara");
        KEYS.put("trader_sprite", "yara");
        KEYS.put("elder_sprite", "elder_kai");
        KEYS.put("coder_sprite", "coder_nia");
    }

    private Sprites() {
    }

    /** Names of all code-drawn sprites. */
    public static List<String> names() {
        return List.copyOf(FRAMES.keySet());
    }

    /** The animation frames of a sprite (one frame for most, two for the player). */
    public static List<String[]> frames(String name) {
        List<String[]> frames = FRAMES.get(name);
        if (frames == null) {
            throw new IllegalArgumentException("No sprite named '" + name + "'");
        }
        return frames;
    }

    /** The sprite name for a key the game sends (e.g. "elder_sprite" -> "elder_kai"), or null if unknown. */
    public static String nameForKey(String spriteKey) {
        return KEYS.get(spriteKey);
    }

    /** The sprite for a map label: "!" and "?" are drawn as marker sprites; any other text returns null. */
    public static String nameForLabel(String label) {
        if ("!".equals(label)) {
            return "exclaim";
        }
        if ("?".equals(label)) {
            return "question";
        }
        return null;
    }
}
