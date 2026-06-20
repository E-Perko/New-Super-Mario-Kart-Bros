package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.*;

// Abstract base class for all ghosts.
// Subclasses override chooseDirection() to implement different personalities.
public abstract class Ghost extends Sprite {

    // --- Frightened mode ---
    protected boolean frightened = false;
    private double frightenTimer = 0;
    public static final double FRIGHTEN_DURATION = 7.0;

    // --- Speed ---
    protected final double baseSpeed; // set once from constructor, never changes

    // --- Spawn control — inactive ghosts skip update and collisions ---
    private boolean active = true;

    // --- Dead state — ghost returns home as floating eyes after being eaten ---
    private boolean dead = false;

    // Current movement direction (in tile units)
    protected int dx = 0, dy = -1;

    // Target pixel toward which the ghost moves; updated each tile
    private double targetX, targetY;
    private boolean hasTarget = false;

    // Home spawn position (ghost house centre)
    protected final int startCol;
    protected final int startRow;

    protected Ghost(GameMap map, GameMap.Tile spawnTile, double speed) {
        super(
            map.spawnCol(spawnTile) * GameMap.TILE,
            map.spawnRow(spawnTile) * GameMap.TILE,
            GameMap.TILE,
            speed
        );
        this.baseSpeed = speed;
        this.startCol  = map.spawnCol(spawnTile);
        this.startRow  = map.spawnRow(spawnTile);
        targetX = x;
        targetY = y;
    }

    // ---------------------------------------------------------------
    // Subclasses implement this to choose a target tile.
    // Return int[]{col, row}.  The engine picks the open neighbour
    // closest to that target (no U-turns allowed).
    // ---------------------------------------------------------------
    protected abstract int[] chooseTarget(Player player, GameMap map);

    public void applySpeedMultiplier(double m) { speed = baseSpeed * m; }
    public boolean isActive()           { return active; }
    public void    setActive(boolean a) { active = a; }
    public boolean isDead()             { return dead; }

    public void kill() {
        dead          = true;
        frightened    = false;
        frightenTimer = 0;
        hasTarget     = false;
    }

    @Override
    public void update(double dt, GameMap map) {
        // Tick the frighten timer even when inactive so all ghost timers stay
        // in sync — prevents the frightened siren from cutting out early when
        // the only active ghost recovers but inactive ghosts still have time left
        if (frightenTimer > 0) {
            frightenTimer -= dt;
            if (frightenTimer <= 0) frightened = false;
        }

        if (!active || currentPlayer == null) return;

        // Dead: float eyes directly toward home, phasing through walls
        if (dead) {
            double pixels = baseSpeed * 2.5 * dt * 60;
            double homeX  = startCol * GameMap.TILE;
            double homeY  = startRow * GameMap.TILE;
            double mx = homeX - x, my = homeY - y;
            double dist = Math.hypot(mx, my);
            if (dist <= pixels) {
                resetPosition();
            } else {
                x += pixels * mx / dist;
                y += pixels * my / dist;
                dx = (Math.abs(mx) >= Math.abs(my)) ? (mx > 0 ? 1 : -1) : 0;
                dy = (Math.abs(my) >  Math.abs(mx)) ? (my > 0 ? 1 : -1) : 0;
            }
            return;
        }

        double pixels = speed * dt * 60;

        // Only pick a new direction once the ghost has reached its target tile centre
        if (!hasTarget) {
            int c = Math.max(0, Math.min(map.cols - 1, col(map)));
            int r = Math.max(0, Math.min(map.rows - 1, row(map)));
            x = map.tileCenterX(c) - size / 2;
            y = map.tileCenterY(r) - size / 2;

            int[] target = chooseTarget(currentPlayer, map);
            int[] next   = bfsDirection(c, r, target[0], target[1], map);
            dx = next[0];
            dy = next[1];
            if (!map.isWall(c + dx, r + dy)) {
                targetX = map.tileCenterX(c + dx) - size / 2;
                targetY = map.tileCenterY(r + dy) - size / 2;
                hasTarget = true;
            }
            // If still no valid target, ghost stays put this frame and retries next frame
        }

        // Move toward target tile centre
        double moveX = targetX - x, moveY = targetY - y;
        double dist  = Math.hypot(moveX, moveY);
        if (dist <= pixels) {
            x = targetX;
            y = targetY;
            hasTarget = false;
        } else {
            x += pixels * moveX / dist;
            y += pixels * moveY / dist;
        }

        // Wrap tunnels — any open edge row/col wraps to the opposite side
        if (x + size < 0 && map.isHorizontalTunnel(row(map))) {
            x = map.width - size;
            hasTarget = false;
        } else if (x > map.width && map.isHorizontalTunnel(row(map))) {
            x = 0;
            hasTarget = false;
        }
        if (y + size < 0 && map.isVerticalTunnel(col(map))) {
            y = map.height - size;
            hasTarget = false;
        } else if (y > map.height && map.isVerticalTunnel(col(map))) {
            y = 0;
            hasTarget = false;
        }
    }

    // Called by the game loop instead of update(dt, map) to supply Player
    public void update(double dt, GameMap map, Player player) {
        // Store player reference for chooseTarget
        this.currentPlayer = player;
        update(dt, map);
    }

    protected Player currentPlayer;

    // BFS shortest-path direction chooser — picks the open neighbour
    // (no reversing) whose tile-distance to target is smallest.
    protected int[] pickDirection(int col, int row, int targetCol, int targetRow, GameMap map) {
        int[][] dirs = {{0,-1},{-1,0},{1,0},{0,1}};
        int[] best = {dx, dy};
        double bestDist = Double.MAX_VALUE;

        for (int[] d : dirs) {
            // No U-turn
            if (d[0] == -dx && d[1] == -dy) continue;
            int nc = col + d[0], nr = row + d[1];
            if (map.isWall(nc, nr)) continue;
            double dist = Math.hypot(nc - targetCol, nr - targetRow);
            if (dist < bestDist) { bestDist = dist; best = d; }
        }
        return best;
    }

    // BFS to find the shortest path — returns the first step as [dc, dr]
    protected int[] bfsDirection(int col, int row, int targetCol, int targetRow, GameMap map) {
        if (col < 0 || col >= map.cols || row < 0 || row >= map.rows) return new int[]{dx, dy};
        if (col == targetCol && row == targetRow) {
            // Already at target — keep going forward if open, else turn
            if (!map.isWall(col + dx, row + dy)) return new int[]{dx, dy};
            int[][] d0 = {{0,-1},{-1,0},{1,0},{0,1}};
            for (int[] d : d0) {
                if (d[0] == -dx && d[1] == -dy) continue;
                if (!map.isWall(col + d[0], row + d[1])) return d;
            }
            return new int[]{-dx, -dy};
        }

        int[][] dirs = {{0,-1},{-1,0},{1,0},{0,1}};
        boolean[][] visited = new boolean[map.rows][map.cols];
        int[][] fromDir = new int[map.rows * map.cols][2];
        Queue<int[]> queue = new LinkedList<>();

        visited[row][col] = true;
        queue.add(new int[]{col, row, -1, -1});

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cc = cur[0], cr = cur[1];
            int fd = cur[2]; // index into dirs for first step

            for (int i = 0; i < dirs.length; i++) {
                int[] d = dirs[i];
                if (d[0] == -dx && d[1] == -dy && cc == col && cr == row) continue;
                int nc = cc + d[0], nr = cr + d[1];
                if (nc < 0 || nc >= map.cols || nr < 0 || nr >= map.rows) continue;
                if (map.isWall(nc, nr) || visited[nr][nc]) continue;
                visited[nr][nc] = true;
                int firstStep = (fd == -1) ? i : fd;
                if (nc == targetCol && nr == targetRow) return dirs[firstStep];
                queue.add(new int[]{nc, nr, firstStep, -1});
            }
        }
        // Fallback: greedy (inline — avoids recursive call through overrides)
        int[] best = null;
        double bestDist = Double.MAX_VALUE;
        for (int[] d : dirs) {
            if (d[0] == -dx && d[1] == -dy) continue;
            int nc = col + d[0], nr = row + d[1];
            if (map.isWall(nc, nr)) continue;
            double dist = Math.hypot(nc - targetCol, nr - targetRow);
            if (dist < bestDist) { bestDist = dist; best = d; }
        }
        // Dead end: no forward option, allow U-turn
        if (best == null) best = new int[]{-dx, -dy};
        return best;
    }

    public void frighten() { frighten(FRIGHTEN_DURATION); }

    public void frighten(double duration) {
        if (dead) return;  // returning eyes can't be frightened
        frightened    = true;
        frightenTimer = duration;
        dx = -dx; dy = -dy;
        hasTarget = false;
    }

    public boolean isFrightened() { return frightened; }

    public void resetPosition() {
        x = startCol * GameMap.TILE;
        y = startRow * GameMap.TILE;
        dx = 0; dy = -1;
        hasTarget = false;
        frightened = false;
        frightenTimer = 0;
        active = true;
        dead   = false;
    }

    // Returns the name shown on the death screen when this ghost catches the player.
    // Override in each subclass to give your ghost a unique name.
    public String getName() { return getClass().getSimpleName(); }

    public boolean collidesWith(Player p) {
        return distanceTo(p.centerX(), p.centerY()) <= GameMap.TILE * 19 / 20.0;
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Dead — show only floating eyes returning to the ghost house
        if (dead) {
            double px = x, py = y, s = size;
            gc.setFill(Color.WHITE);
            gc.fillOval(px + s * 0.15, py + s * 0.2,  s * 0.3,  s * 0.35);
            gc.fillOval(px + s * 0.55, py + s * 0.2,  s * 0.3,  s * 0.35);
            gc.setFill(Color.web("#0000cc"));
            gc.fillOval(px + s * 0.22 + dx * 3, py + s * 0.27 + dy * 3, s * 0.15, s * 0.18);
            gc.fillOval(px + s * 0.62 + dx * 3, py + s * 0.27 + dy * 3, s * 0.15, s * 0.18);
            return;
        }

        // Custom sprite image — only used in normal state; frightened still shows built-in blue
        if (spriteImage != null && !frightened) {
            gc.drawImage(spriteImage, x, y, size, size);
            return;
        }

        Color body = frightened
            ? (frightenTimer < 2.0 && (int)(frightenTimer * 5) % 2 == 0
                ? Color.WHITE : Color.web("#0000cc"))
            : getBodyColor();

        double px = x, py = y, s = size;

        // Body
        gc.setFill(body);
        gc.fillArc(px, py, s, s, 0, 180, javafx.scene.shape.ArcType.CHORD);
        gc.fillRect(px, py + s / 2, s, s / 2);

        // Wavy bottom
        gc.setFill(Color.BLACK);
        int waves = 3;
        double ww = s / waves;
        for (int i = 0; i < waves; i++) {
            gc.fillOval(px + i * ww, py + s - ww * 0.6, ww, ww * 0.7);
        }
        gc.setFill(body);
        for (int i = 0; i < waves; i++) {
            gc.fillOval(px + i * ww + ww * 0.15, py + s - ww * 0.5, ww * 0.7, ww * 0.6);
        }

        // Eyes (hidden when frightened)
        if (!frightened) {
            gc.setFill(Color.WHITE);
            gc.fillOval(px + s * 0.2, py + s * 0.2, s * 0.25, s * 0.3);
            gc.fillOval(px + s * 0.55, py + s * 0.2, s * 0.25, s * 0.3);
            gc.setFill(Color.web("#0000cc"));
            gc.fillOval(px + s * 0.27 + dx * 3, py + s * 0.27 + dy * 3, s * 0.13, s * 0.15);
            gc.fillOval(px + s * 0.62 + dx * 3, py + s * 0.27 + dy * 3, s * 0.13, s * 0.15);
        }
    }

    // Subclasses return their unique body colour
    protected abstract Color getBodyColor();
}
