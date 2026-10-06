package isla.world;

import static org.junit.jupiter.api.Assertions.*;

import isla.NPC;
import isla.Player;
import isla.Quest;
import isla.QuestGiver;
import isla.RecordingRenderer;
import isla.Tool;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameWorldTest {

    private static final TileMap MAP = TileMap.parse(List.of(
            "gggggggwgg",
            "gggggjgwgg",
            "ggggggggwg",
            "gggggggggg",
            "gggggggggg",
            "gggggggggg"));

    private Player player(int x, int y) {
        Player p = new Player(1, "Pj", 50, 100, 3);
        p.setPosition(x, y);
        return p;
    }

    private QuestGiver npcAt(int x, int y) {
        return new QuestGiver(1, x, y, "elder_sprite", "Hi", new Quest(1, 1, "q", 5, null));
    }

    private GameWorld world(Player p, NPC... npcs) {
        return new GameWorld(MAP, p, new ArrayList<>(List.of(npcs)));
    }

    @Test
    void moveOntoGrass_moves() {
        Player p = player(1, 1);
        assertEquals(MoveResult.MOVED, world(p).movePlayer(1, 0));
        assertEquals(2, p.getX());
        assertEquals(1, p.getY());
    }

    @Test
    void moveIntoWater_blockedNoStaminaCost() {
        Player p = player(6, 0);
        GameWorld w = world(p);
        for (int i = 0; i < 25; i++) {
            assertEquals(MoveResult.BLOCKED, w.movePlayer(1, 0));
        }
        assertEquals(6, p.getX());
        assertEquals(100, p.getStamina());
    }

    @Test
    void moveOffMapEdge_blocked() {
        Player p = player(0, 0);
        GameWorld w = world(p);
        assertEquals(MoveResult.BLOCKED, w.movePlayer(-1, 0));
        assertEquals(MoveResult.BLOCKED, w.movePlayer(0, -1));
        assertEquals(0, p.getX());
        assertEquals(0, p.getY());
    }

    @Test
    void moveIntoNpc_blocked() {
        Player p = player(1, 3);
        assertEquals(MoveResult.BLOCKED, world(p, npcAt(2, 3)).movePlayer(1, 0));
        assertEquals(1, p.getX());
    }

    @Test
    void exhaustedPlayer_tooTired_positionUnchanged() {
        Player p = player(1, 3);
        p.expendStamina(100);
        assertEquals(MoveResult.TOO_TIRED, world(p).movePlayer(1, 0));
        assertEquals(1, p.getX());
    }

    @Test
    void jungleBlockedUntilToolUsed() {
        Player p = player(4, 1);
        GameWorld w = world(p);
        assertEquals(MoveResult.BLOCKED, w.movePlayer(1, 0));
        new Tool(4, "Rusty Machete", 30, "Jungle Path").use(p);
        assertEquals(MoveResult.MOVED, w.movePlayer(1, 0));
        assertEquals(5, p.getX());
    }

    @Test
    void interactableNear_findsAdjacentNpcOnly() {
        QuestGiver npc = npcAt(3, 3);
        Player p = player(2, 3);
        assertTrue(world(p, npc).interactableNear().isPresent());
        assertTrue(world(player(3, 4), npc).interactableNear().isPresent());
        assertTrue(world(player(1, 3), npc).interactableNear().isEmpty());
        assertTrue(world(player(2, 4), npc).interactableNear().isEmpty()); // diagonal
    }

    @Test
    void render_cameraClampedToMap_andPlayerDrawnLast() {
        RecordingRenderer r = new RecordingRenderer();
        world(player(9, 5), npcAt(8, 5)).render(r, 4, 3);
        assertEquals("camera 6,3", r.calls.get(0));
        assertEquals(12, r.count("tile "));
        assertEquals("sprite 9,5,player_sprite", r.lastSprite());

        RecordingRenderer corner = new RecordingRenderer();
        world(player(0, 0)).render(corner, 4, 3);
        assertEquals("camera 0,0", corner.calls.get(0));

        RecordingRenderer mid = new RecordingRenderer();
        world(player(5, 3)).render(mid, 4, 3);
        assertEquals("camera 3,2", mid.calls.get(0));
    }

    @Test
    void render_skipsNpcsOutsideView() {
        RecordingRenderer r = new RecordingRenderer();
        world(player(0, 0), npcAt(9, 5)).render(r, 4, 3);
        assertEquals(0, r.count("label "));
        assertEquals(1, r.count("sprite "));
    }
}
