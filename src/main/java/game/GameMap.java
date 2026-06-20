package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.EnumMap;
import java.util.EnumSet;

// Pac-Man maze.
//
// To design your own maze, edit DEFAULT_LAYOUT below (or pass any Tile[][]
// to the GameMap(Tile[][]) constructor to support per-level layouts).
// Rules you must follow:
//   - Each spawn tile (PL, G0–G3, BN) must appear exactly once
//   - Any open tile on a left/right edge must have an open tile on the
//     opposite edge (horizontal tunnel); same rule for top/bottom edges
public class GameMap {

    // TILE is a rendering constant — always 28 px regardless of maze size
    public static final int TILE = 64;

    // Instance dimensions — derived from whichever layout was passed in
    public final int cols;
    public final int rows;
    public final int width;
    public final int height;

    // ---------------------------------------------------------------
    // Tile types
    // ---------------------------------------------------------------
    public enum Tile {
        W,            // Wall
        D,            // Dot
        P,            // Power pellet
        E,            // Empty
        B,            // Player exclusive wall
        SPAWN_PLAYER, // Player spawn position
        SPAWN_G0,     // Ghost 0 (Shadow) spawn
        SPAWN_G1,     // Ghost 1 (Patrol) spawn
        SPAWN_G2,     // Ghost 2 (Shy) spawn
        SPAWN_G3,     // Ghost 3 (Ambush) spawn
        SPAWN_BONUS   // Bonus item spawn
    }

    // Short aliases so the maze array below stays readable
    private static final Tile W  = Tile.W,  D  = Tile.D,  P  = Tile.P,  E  = Tile.E, B = Tile.B;
    private static final Tile PL = Tile.SPAWN_PLAYER;
    private static final Tile G0 = Tile.SPAWN_G0, G1 = Tile.SPAWN_G1;
    private static final Tile G2 = Tile.SPAWN_G2, G3 = Tile.SPAWN_G3;
    private static final Tile BN = Tile.SPAWN_BONUS;

    // Spawn tiles are walkable — treated as empty during gameplay
    private static final EnumSet<Tile> SPAWN_TILES = EnumSet.of(
        Tile.SPAWN_PLAYER, Tile.SPAWN_G0, Tile.SPAWN_G1,
        Tile.SPAWN_G2, Tile.SPAWN_G3, Tile.SPAWN_BONUS
    );

    public static boolean isSpawnTile(Tile t) { return SPAWN_TILES.contains(t); }

    // ---------------------------------------------------------------
    // Default maze layout  (W=wall  D=dot  P=power pellet  E=empty)
    // Spawn positions are marked with: PL (player), G0-G3 (ghosts), BN (bonus)
    // Move any spawn tile freely — just keep it on an open (non-W) tile.
    // Pass a different Tile[][] to GameMap(layout) for a custom maze.
    // ---------------------------------------------------------------
    public static final Tile[][] DEFAULT_LAYOUT = {
        {W,  D,  W,  W,  W,  W,  W,  D,  W,  W,  W,  W,  W,  D,  W},
        {W,  D,  D,  D,  D,  D,  D,  D,  D,  D,  D,  D,  D,  D,  W},
        {W,  D,  W,  W,  D,  W,  W,  W,  W,  W,  D,  W,  W,  D,  W},
        {W,  D,  D,  D,  D,  B,  G1, G0, G2, B,  D,  D,  D,  D,  W},
        {W,  D,  W,  D,  W,  E,  G3, E,  E,  E,  W,  D,  W,  D,  W},
        {D,  D,  D,  D,  W,  W,  W,  W,  W,  W,  W,  D,  D,  D,  D},
        {W,  D,  W,  D,  D,  D,  D,  BN, D,  D,  D,  D,  W,  D,  W},
        {W,  D,  D,  D,  W,  W,  D,  PL, D,  W,  W,  D,  D,  D,  W},
        {W,  D,  W,  D,  D,  D,  D,  D,  D,  D,  D,  D,  W,  D,  W},
        {W,  D,  W,  W,  W,  W,  W,  D,  W,  W,  W,  W,  W,  D,  W},
    };

    // Original layout (never modified) and working copy (dots consumed as eaten)
    private final Tile[][] layout;
    private final Tile[][] state;

    // Spawn positions discovered by scanning the layout
    private final EnumMap<Tile, int[]> spawnPositions = new EnumMap<>(Tile.class);

    private int dotsRemaining;
    private int totalDots;

    public GameMap() { this(DEFAULT_LAYOUT); }

    public GameMap(Tile[][] layout) {
        this.rows   = layout.length;
        this.cols   = layout[0].length;
        this.width  = cols * TILE;
        this.height = rows * TILE;
        this.layout = layout;
        this.state  = new Tile[rows][cols];
        scanSpawns();
        validateLayout();
        reset();
    }

    // ---------------------------------------------------------------
    // Spawn position lookup — call these to find where each spawn is
    // ---------------------------------------------------------------
    public int spawnCol(Tile spawnTile) { return spawnPositions.get(spawnTile)[0]; }
    public int spawnRow(Tile spawnTile) { return spawnPositions.get(spawnTile)[1]; }

    // ---------------------------------------------------------------
    // Scan the layout once to record each spawn tile's position
    // ---------------------------------------------------------------
    private void scanSpawns() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Tile t = layout[r][c];
                if (isSpawnTile(t)) spawnPositions.put(t, new int[]{c, r});
            }
        }
        for (Tile t : SPAWN_TILES) {
            if (!spawnPositions.containsKey(t))
                throw new IllegalStateException(
                    "Maze design error: required spawn tile " + t + " not found in layout.");
        }
    }

    // ---------------------------------------------------------------
    // Validate tunnel symmetry
    // ---------------------------------------------------------------
    private void validateLayout() {
        for (int c = 0; c < cols; c++) {
            if ((layout[0][c] != Tile.W) != (layout[rows - 1][c] != Tile.W))
                throw new IllegalStateException(
                    "Maze design error: col " + c + " has an open top edge but not bottom (or vice versa). " +
                    "Vertical tunnels must be open on both sides.");
        }
        for (int r = 0; r < rows; r++) {
            if ((layout[r][0] != Tile.W) != (layout[r][cols - 1] != Tile.W))
                throw new IllegalStateException(
                    "Maze design error: row " + r + " has an open left edge but not right (or vice versa). " +
                    "Horizontal tunnels must be open on both sides.");
        }
    }

    /** True when row {@code r} has open tiles on both the left and right edges. */
    public boolean isHorizontalTunnel(int r) {
        return r >= 0 && r < rows && layout[r][0] != Tile.W;
    }

    /** True when col {@code c} has open tiles on both the top and bottom edges. */
    public boolean isVerticalTunnel(int c) {
        return c >= 0 && c < cols && layout[0][c] != Tile.W;
    }

    public final void reset() {
        dotsRemaining = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Spawn tiles are walkable empty floor during gameplay
                state[r][c] = isSpawnTile(layout[r][c]) ? Tile.E : layout[r][c];
                if (layout[r][c] == Tile.D || layout[r][c] == Tile.P) dotsRemaining++;
            }
        }
        totalDots = dotsRemaining;
    }

    public Tile getTile(int col, int row) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return Tile.W;
        return state[row][col];
    }

    public boolean isWall(int col, int row) {
        return getTile(col, row) == Tile.W;
    }

    public boolean isPLayerWall(int col, int row) {
        return getTile(col, row) == Tile.B;
    }

    // Returns 0 if nothing eaten, 10 for dot, 50 for power pellet
    public int eatDot(int col, int row) {
        Tile t = getTile(col, row);
        if (t == Tile.D) { state[row][col] = Tile.E; dotsRemaining--; return 10; }
        if (t == Tile.P) { state[row][col] = Tile.E; dotsRemaining--; return 50; }
        return 0;
    }

    public boolean isPowerPellet(int col, int row) {
        return getTile(col, row) == Tile.P;
    }

    public boolean allDotsEaten() { return dotsRemaining == 0; }
    public int getDotsRemaining() { return dotsRemaining; }
    public int getTotalDots()     { return totalDots; }

    public int    pixelToCol(double px) { return (int)(px / TILE); }
    public int    pixelToRow(double py) { return (int)(py / TILE); }
    public double tileCenterX(int col)  { return col * TILE + TILE / 2.0; }
    public double tileCenterY(int row)  { return row * TILE + TILE / 2.0; }

    public void draw(GraphicsContext gc) {
        draw(gc, Color.web("#202090")); // change this hex code to pick your wall color
    }

    public void draw(GraphicsContext gc, Color wallColor) {
        gc.setFill(Color.web("#000000"));
        gc.fillRect(0, 0, width, height);

        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                double px = c * TILE;
                double py = r * TILE;
                Tile t = state[r][c];

                if (t == Tile.W)
                {
                    gc.setFill(wallColor);
                    gc.fillRoundRect(px + 1, py + 1, TILE - 2, TILE - 2, 6, 6);
                }
                else if (t == Tile.D)
                {
                    double cx = px + TILE / 2.0,  cy = py + TILE / 2.0;
                    gc.setFill(Color.web("#885544"));
                    gc.fillOval(cx - 3, cy - 3, TILE / 6.0, TILE / 6.0);
                }
                else if (t == Tile.P)
                {
                    double cx = px + TILE / 2.0,  cy = py + TILE / 2.0;
                    gc.fillOval(cx - 7, cy - 7, TILE / 3.0, TILE / 3.0);
                }
                else if (t == Tile.B)
                {
                    gc.setFill(wallColor);
                    gc.fillRoundRect(px + 1, py + 1, TILE - 2, TILE - 2, 6, 6);
                }
            }
        }
    }
}
