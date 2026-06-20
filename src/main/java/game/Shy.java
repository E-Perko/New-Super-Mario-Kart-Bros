package game;

import javafx.scene.paint.Color;

// Shy — runs away unless cornered (few open neighbours), then attacks.
public class Shy extends Ghost {

    public Shy(GameMap map) {
        super(map, GameMap.Tile.SPAWN_G2, 1.5);
    }

    @Override
    public String getName() { return "Shy"; } // give your ghost a name

    @Override
    protected int[] chooseTarget(Player player, GameMap map) {
        if (!frightened) {
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

    // When chooseTarget() is working, add this ghost to the list in PelletPursuitDemo.java:
    //   new Shy(map)

    @Override
    protected Color getBodyColor() { return Color.web("#00ffff"); }
}
