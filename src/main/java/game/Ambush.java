package game;

import javafx.scene.paint.Color;

// Ambush — tries to cut the player off by targeting ahead of where they are heading.
public class Ambush extends Ghost {

    // Number of tiles ahead of the player's current direction to target.
    // Try values between 2 and 8.
    private static final int LOOK_AHEAD = 3;

    public Ambush(GameMap map) {
        super(map, GameMap.Tile.SPAWN_G3, 1.7);
    }

    @Override
    public String getName() { return "Ambush"; } // give your ghost a name

    @Override
    protected int[] chooseTarget(Player player, GameMap map) {
        // TODO (Base): Implement Ambush's personality.
        //
        // Ambush targets a point LOOK_AHEAD tiles ahead of the player's current
        // heading. Use player.getDx() and player.getDy() to find the heading,
        // then project that many tiles forward from the player's tile position.
        //
        // Make sure the target is always inside the maze — clamp the column to
        // [1, map.cols-2] and the row to [1, map.rows-2] so the target is never
        // a border wall.
        //
        // When frightened, target a corner of the maze instead.
        //
        // How to verify: run the game and move in one direction — the orange ghost
        // should approach from in front of you rather than chasing from behind.
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
        return new int[]{ Math.clamp(player.col(map) + (int)(LOOK_AHEAD * player.getNextDx()), 1, map.cols-2), Math.clamp(player.row(map) + (int)(LOOK_AHEAD * player.getNextDy()), 1, map.rows-2) }; // placeholder — replace this
    }

    // When chooseTarget() is working, add this ghost to the list in PelletPursuitDemo.java:
    //   new Ambush(map)

    @Override
    protected Color getBodyColor() { return Color.web("#ffb852"); }
}
