package game;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class GraphicsManager {
    ImageView[] imgView = new ImageView[3];
    Image[] image = new Image[3];
    Pane[] pane = new Pane[3];

    public void displayImg(String img, double width, double xPos, double yPos, int imgNum, StackPane root) {
        imgView[imgNum] = new ImageView();
        image[imgNum] = new Image("/" + img.toLowerCase() + ".png", GameMap.TILE * 30, 0, true, false);
        imgView[imgNum].setFitWidth(GameMap.TILE * width);
        imgView[imgNum].setPreserveRatio(true);
        imgView[imgNum].setX(GameMap.TILE * xPos);
        imgView[imgNum].setY(GameMap.TILE * yPos);
        imgView[imgNum].setImage(image[imgNum]);
        pane[imgNum] = new Pane(imgView[imgNum]);
        root.getChildren().addAll(pane[imgNum]);
    }

    public void setImgX(int imgNum, int x) {
        imgView[imgNum].setX(x);
    }

    public void setImgY(int imgNum, int y) {
        imgView[imgNum].setY(y);
    }

    public void deleteImage(int imgNum) {
        imgView[imgNum].setImage(null);
    }
}
