package game;

public class ScoreNode {
    int score;
    int level;
    ScoreNode left;
    ScoreNode right;

    ScoreNode(int score, int level) {
        this.score = score;
        this.level = level;
    }
}
