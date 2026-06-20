package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

public class Player extends Sprite {

    // Requested direction from keyboard; actual direction changes only at tile centres
    private int dx = 0, dy = 0;
    private int nextDx = 0, nextDy = 0;

    // Mouth animation
    private double mouthAngle = 45;
    private double mouthDir   = -1;

    // Change this to any Color to customize your player's appearance
    protected Color bodyColor = Color.YELLOW;

    private final int startCol;
    private final int startRow;

    public Player(GameMap map) {
        super(
            map.spawnCol(GameMap.Tile.SPAWN_PLAYER) * GameMap.TILE,
            map.spawnRow(GameMap.Tile.SPAWN_PLAYER) * GameMap.TILE,
            GameMap.TILE,
            GameMap.TILE / 15.0
        );
        this.startCol = map.spawnCol(GameMap.Tile.SPAWN_PLAYER);
        this.startRow = map.spawnRow(GameMap.Tile.SPAWN_PLAYER);
    }

    public void handleKey(KeyCode key) {
        switch (key) {
            case RIGHT, D -> { nextDx = 1; nextDy = 0; }
            case LEFT, A -> { nextDx = -1; nextDy = 0; }
            case UP, W -> { nextDx = 0; nextDy = -1; }
            case DOWN, S -> { nextDx = 0; nextDy = 1; }
            default -> {}
        }
    }

    public int getNextDx() {
        return nextDx;
    }

    public int getNextDy() {
        return nextDy;
    }

    @Override
    public void update(double dt, GameMap map) {
        double pixels = speed * dt * 60;

        // Try to honor queued turn at tile center
        if (isAligned(map)) {
            int col = col(map), row = row(map);
            if (!map.isWall(col + nextDx, row + nextDy) && !map.isWall(col + nextDx, row + nextDy)) {
                if (nextDx != dx || nextDy != dy) {
                    // Snap exactly to tile center before turning so the new
                    // corridor is entered perfectly centred
                    x = map.tileCenterX(col) - size / 2.0;
                    y = map.tileCenterY(row) - size / 2.0;
                }
                dx = nextDx;
                dy = nextDy;
            }
        }

        // Move if the next tile in current direction is open.
        // Allow out-of-bounds columns so the player can exit the tunnel.
        double nx = x + dx * pixels;
        double ny = y + dy * pixels;
        int nc = map.pixelToCol(nx + size / 2 + dx * (size / 2 - 2));
        int nr = map.pixelToRow(ny + size / 2 + dy * (size / 2 - 2));

        // TODO (Phase 1): Decide whether the player can enter tile (nc, nr).
        // Use map.isWall(nc, nr) to check the destination. Out-of-bounds tiles
        // should be treated as open so the player can exit through tunnel edges.
        //
        // Hint: (nc < 0 || nc >= map.cols || nr < 0 || nr >= map.rows) || !map.isWall(nc, nr)
        //
        // Add a comment explaining why the out-of-bounds check is needed.
        boolean canMove = (nc < 0 || nc >= map.cols || nr < 0 || nr >= map.rows) || (!map.isWall(nc, nr) && !map.isPLayerWall(nc, nr));
        if (canMove) {
            x = nx;
            y = ny;
        }

        // Continuously correct the perpendicular axis so straight movement
        // never drifts off-center in a corridor
        if (dx != 0) y = map.tileCenterY(row(map)) - size / 2.0;
        if (dy != 0) x = map.tileCenterX(col(map)) - size / 2.0;

        // Wrap tunnels — any open edge row/col wraps to the opposite side
        if (x + size < GameMap.TILE * -1      && map.isHorizontalTunnel(row(map))) x = map.width - size + GameMap.TILE;
        if (x > map.width + GameMap.TILE    && map.isHorizontalTunnel(row(map))) x = GameMap.TILE * -1;
        if (y + size < GameMap.TILE * -1      && map.isVerticalTunnel(col(map)))   y = map.height - size + GameMap.TILE;
        if (y > map.height + GameMap.TILE   && map.isVerticalTunnel(col(map)))   y = GameMap.TILE * -1;

        // Mouth animates only while moving; freezes when blocked
        if (canMove) {
            mouthAngle += mouthDir * 4;
            if (mouthAngle <= 5)  { mouthAngle = 5;  mouthDir =  1; }
            if (mouthAngle >= 45) { mouthAngle = 45; mouthDir = -1; }
        }
    }

    // True when the sprite is close enough to a tile center to allow turning
    private boolean isAligned(GameMap map) {
        double cx = centerX(), cy = centerY();
        double tx = map.tileCenterX(col(map));
        double ty = map.tileCenterY(row(map));
        return Math.abs(cx - tx) < speed + 1 && Math.abs(cy - ty) < speed + 1;
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Rotation based on movement direction
        double angle = 0;
        if      (dx ==  1) angle = 0;
        else if (dx == -1) angle = 180;
        else if (dy == -1) angle = 270;
        else if (dy ==  1) angle = 90;

        gc.save();
        gc.translate(x + (GameMap.TILE / 2.0), y + (GameMap.TILE / 2.0));
        gc.rotate(angle);

        if (spriteImage != null) {
            gc.drawImage(spriteImage, -size / 2, -size / 2, size, size);
        } else {
            gc.translate(-size / 2, -size / 2);
            gc.setFill(bodyColor);
            gc.fillArc(0, 0, size, size, mouthAngle / 2, 360 - mouthAngle, javafx.scene.shape.ArcType.ROUND);
        }

        gc.restore();
    }

    public int getDx() { return dx; }
    public int getDy() { return dy; }

    public void resetPosition() {
        x = startCol * GameMap.TILE;
        y = startRow * GameMap.TILE;
        dx = 0; dy = 0;
        nextDx = 0; nextDy = 0;
    }
}
