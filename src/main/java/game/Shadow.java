package game;

import javafx.scene.paint.Color;

// Shadow — the chaser.  Always targets the player's exact tile using BFS.
// This ghost is provided as a WORKED EXAMPLE for students to study.
public class Shadow extends Ghost {

    public Shadow(GameMap map) {
        super(map, GameMap.Tile.SPAWN_G0, 1.8);
    }

    @Override
    public String getName() { return "Shadow"; }

    @Override
    protected int[] chooseTarget(Player player, GameMap map) {
        if (frightened) {
            // Run away: target the corner farthest from the player
            int pc = player.col(map), pr = player.row(map);
            int[] corner = (pc < map.cols / 2)
                ? (pr < map.rows / 2 ? new int[]{map.cols - 2, map.rows - 2}
                                     : new int[]{map.cols - 2, 1})
                : (pr < map.rows / 2 ? new int[]{1, map.rows - 2}
                                     : new int[]{1, 1});
            return corner;
        }
        // Chase: target the player's current tile (BFS shortest path)
        return new int[]{ player.col(map), player.row(map) };
    }

    @Override
    protected Color getBodyColor() { return Color.web("#ff0000"); }
}
