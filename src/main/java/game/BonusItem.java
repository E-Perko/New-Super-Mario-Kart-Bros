package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * BonusItem — a stationary collectible that appears on the map for a limited time.
 *
 * To create your own bonus item, extend this class and implement:
 *   - getPoints()  — points awarded when the player collects it
 *   - getLabel()   — short display string drawn on the item (e.g. "CHERRY")
 *   - getColor()   — fill color of the circle
 *
 * Example subclass: {@link Cherry}
 */
public abstract class BonusItem extends Sprite {

    private double lifetime; // seconds remaining before the item disappears

    /**
     * @param pixelX    pixel x position (top-left corner, same convention as Sprite)
     * @param pixelY    pixel y position (top-left corner)
     * @param size      diameter of the item in pixels
     * @param lifetime  how many seconds the item stays on screen
     */
    protected BonusItem(double pixelX, double pixelY, double size, double lifetime) {
        super(pixelX, pixelY, size, 0); // bonus items never move
        this.lifetime = lifetime;
    }

    // -----------------------------------------------------------------------
    // Abstract interface — students implement these three methods
    // -----------------------------------------------------------------------

    /** Points awarded to the player upon collection. */
    public abstract int getPoints();

    /** Short string displayed on the item, e.g. "200" or "CHERRY". */
    public abstract String getLabel();

    /** Fill color of the circular icon. */
    public abstract Color getColor();

    // -----------------------------------------------------------------------
    // Sprite contract
    // -----------------------------------------------------------------------

    /**
     * Counts down the lifetime timer. BonusItems do not move.
     */
    @Override
    public void update(double dt, GameMap map) {
        if (lifetime > 0) {
            lifetime -= dt;
            if (lifetime < 0) lifetime = 0;
        }
    }

    /**
     * Draws a filled circle in getColor() with getLabel() centered on it in
     * small white text.
     */
    @Override
    public void draw(GraphicsContext gc) {
        double cx = centerX();
        double cy = centerY();
        double r  = size / 2.0;

        // Filled circle
        gc.setFill(getColor());
        gc.fillOval(x, y, size, size);

        // Centered label
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", Math.max(8, size * 0.35)));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(getLabel(), cx, cy + size * 0.12);
    }

    // -----------------------------------------------------------------------
    // Collision & state helpers
    // -----------------------------------------------------------------------

    /**
     * Returns true when this item's center is within 80 % of its radius from
     * the player's center — a forgiving hitbox so collection feels natural.
     */
    public boolean collidesWith(Player p) {
        return distanceTo(p.centerX(), p.centerY()) < size * 0.8;
    }

    /** Returns true when the countdown has reached zero. */
    public boolean isExpired() {
        return lifetime <= 0;
    }

    /** Remaining lifetime in seconds (read-only convenience accessor). */
    public double getLifetime() {
        return lifetime;
    }
}

// ---------------------------------------------------------------------------
// Worked example — students may use this as a model for their own items
// ---------------------------------------------------------------------------

/**
 * Cherry — the classic Pac-Man stage-1 bonus fruit.
 * Worth 200 points, shown as a red circle labelled "200", lasts 8 seconds.
 */
class Cherry extends BonusItem {

    public Cherry(double pixelX, double pixelY) {
        super(pixelX, pixelY, GameMap.TILE, 8.0);
    }

    @Override public int    getPoints() { return 200; }
    @Override public String getLabel()  { return "200"; }
    @Override public Color  getColor()  { return Color.RED; }
}
