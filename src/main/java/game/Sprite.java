package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public abstract class Sprite {

    protected double x, y;
    protected final double size;
    protected double speed;

    protected Sprite(double x, double y, double size, double speed) {
        this.x     = x;
        this.y     = y;
        this.size  = size;
        this.speed = speed;
    }

    public abstract void update(double dt, GameMap map);
    public abstract void draw(GraphicsContext gc);

    public double centerX() { return x + size / 2; }
    public double centerY() { return y + size / 2; }

    public double distanceTo(double px, double py) {
        return Math.hypot(centerX() - px, centerY() - py);
    }

    // Optional custom sprite image. Set this in a subclass constructor to
    // replace the default programmatic drawing with an image.
    protected Image spriteImage = null;

    // Loads an image from src/main/resources. Use a path like "/game/images/mysprite.png".
    // Returns null silently if the file is missing, so the default drawing is used instead.
//    protected Image loadImage(String resourcePath) {
//        try {
//            return new Image(getClass().getResourceAsStream(resourcePath));
//        } catch (Exception ignored) {
//            return null;
//        }
//    }

    // Tile the center of this sprite currently occupies
    public int col(GameMap map) { return map.pixelToCol(centerX()); }
    public int row(GameMap map) { return map.pixelToRow(centerY()); }
}
