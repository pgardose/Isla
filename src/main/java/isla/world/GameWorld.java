package isla.world;

import isla.NPC;
import isla.Player;
import isla.Renderer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The playable island: a TileMap, the Player and the NPCs standing on it.
 * Owns the rules for walking (bounds, terrain, locked areas, NPCs in the way,
 * exhaustion) and for finding who the player can talk to. Contains no GUI
 * code; it draws itself through the Renderer interface.
 */
public class GameWorld {

    private final TileMap map;
    private final Player player;
    private final List<NPC> npcs;

    public GameWorld(TileMap map, Player player, List<NPC> npcs) {
        this.map = map;
        this.player = player;
        this.npcs = npcs;
    }

    public TileMap getMap() {
        return map;
    }

    public Player getPlayer() {
        return player;
    }

    public List<NPC> getNpcs() {
        return Collections.unmodifiableList(npcs);
    }

    public Optional<NPC> npcAt(int x, int y) {
        return npcs.stream().filter(n -> n.getX() == x && n.getY() == y).findFirst();
    }

    /**
     * Tries to move the player one tile. A blocked move (water, edge, locked
     * jungle, an NPC) changes nothing and costs no stamina. An exhausted
     * player cannot walk until they rest or use a consumable.
     */
    public MoveResult movePlayer(int dx, int dy) {
        int nx = player.getX() + dx;
        int ny = player.getY() + dy;
        if (!map.canWalk(nx, ny, player.getUnlockedAreas()) || npcAt(nx, ny).isPresent()) {
            return MoveResult.BLOCKED;
        }
        if (player.isExhausted()) {
            return MoveResult.TOO_TIRED;
        }
        player.move(dx, dy);
        return MoveResult.MOVED;
    }

    /** An NPC standing directly next to the player (up, down, left or right), if any. */
    public Optional<NPC> interactableNear() {
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] d : dirs) {
            Optional<NPC> npc = npcAt(player.getX() + d[0], player.getY() + d[1]);
            if (npc.isPresent()) {
                return npc;
            }
        }
        return Optional.empty();
    }

    /** Draws the visible part of the island, then NPCs, then the player on top. */
    public void render(Renderer renderer, int viewCols, int viewRows) {
        int camCol = clamp(player.getX() - viewCols / 2, 0, Math.max(0, map.getWidth() - viewCols));
        int camRow = clamp(player.getY() - viewRows / 2, 0, Math.max(0, map.getHeight() - viewRows));
        renderer.setCamera(camCol, camRow);

        for (int y = camRow; y < camRow + viewRows && y < map.getHeight(); y++) {
            for (int x = camCol; x < camCol + viewCols && x < map.getWidth(); x++) {
                renderer.drawTile(x, y, map.tileAt(x, y).getColor());
            }
        }
        for (NPC npc : npcs) {
            boolean visible = npc.getX() >= camCol && npc.getX() < camCol + viewCols
                    && npc.getY() >= camRow && npc.getY() < camRow + viewRows;
            if (visible) {
                npc.render(renderer);
            }
        }
        player.render(renderer);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
