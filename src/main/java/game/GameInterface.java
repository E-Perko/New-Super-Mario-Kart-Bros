package game;

import javafx.scene.canvas.GraphicsContext;

public class GameInterface {
    public static int selectX = 0;
    public static int selectY = 0;

    public static String[][] battleMoves = new String[2][2];

    private static String playerMove;

    public static int battleState = 0;
    // 0: Initial Options
    // 1: Move Select
    // 2: Selected Move
    // 3: Miss
    // 4: Type Effectiveness
    // 5: Lower Stats
    // 6: Critical Hit

    public static final String[] statNames = {"Attack", "Defense", "Special Attack", "Special Defense", "Speed"};

    public static void renderBattle(GraphicsContext gc) {
        switch (battleState) {
            case 0 -> {
                gc.fillText("What will", GameMap.TILE, GameMap.TILE * 8);
                gc.fillText(Battle.player.pokemon[0] + " do?", GameMap.TILE, GameMap.TILE * 9);
                gc.fillArc((GameMap.TILE * 8) + (GameMap.TILE * selectX * 3.5), (GameMap.TILE * 7.75) + (GameMap.TILE * selectY), 10, 10, 0, 360, javafx.scene.shape.ArcType.ROUND);
                gc.fillText("Fight", GameMap.TILE * 8.5, GameMap.TILE * 8);
                gc.fillText("Bag", GameMap.TILE * 12, GameMap.TILE * 8);
                gc.fillText("Pokémon", GameMap.TILE * 8.5, GameMap.TILE * 9);
                gc.fillText("Run", GameMap.TILE * 12, GameMap.TILE * 9);
            }
            case 1 -> {
                gc.fillArc((GameMap.TILE / 2.0) + (GameMap.TILE * selectX * 5.5), (GameMap.TILE * 7.75) + (GameMap.TILE * selectY), 10, 10, 0, 360, javafx.scene.shape.ArcType.ROUND);
                gc.fillText(battleMoves[0][0], GameMap.TILE, GameMap.TILE * 8);
                gc.fillText(battleMoves[0][1], GameMap.TILE * 6.5, GameMap.TILE * 8);
                gc.fillText(battleMoves[1][0], GameMap.TILE, GameMap.TILE * 9);
                gc.fillText(battleMoves[1][1], GameMap.TILE * 6.5, GameMap.TILE * 9);
            }
            case 2 -> {
                if (Battle.turn % 2 == Battle.turnOrder) {
                    gc.fillText( Battle.player.pokemon[0] + " used " + playerMove + "!", GameMap.TILE, GameMap.TILE * 8);
                } else {
                    gc.fillText("Foe " + Battle.opponent.pokemon[0] + " used " + Battle.opponent.moves[0][0] + "!", GameMap.TILE, GameMap.TILE * 8);
                }
            }
            case 3 -> {
                if (Battle.turn % 2 == Battle.turnOrder) {
                    gc.fillText(Battle.opponent.pokemon[0] + " avoided the attack!", GameMap.TILE, GameMap.TILE * 8);
                } else {
                    gc.fillText(Battle.player.pokemon[0] + " avoided the attack!", GameMap.TILE, GameMap.TILE * 8);
                }
            }
            case 4 -> {
                if (Battle.typeEffect > 1) {
                    gc.fillText("It's super effective!", GameMap.TILE, GameMap.TILE * 8);
                } else if (Battle.typeEffect == 0) {
                    if (Battle.turn % 2 == Battle.turnOrder) {
                        gc.fillText("It doesn't affect " + Battle.opponent.pokemon[0] + "...", GameMap.TILE, GameMap.TILE * 8);
                    } else {
                        gc.fillText("It doesn't affect " + Battle.player.pokemon[0] + "...", GameMap.TILE, GameMap.TILE * 8);
                    }
                } else {
                    gc.fillText("It's not very effective...", GameMap.TILE, GameMap.TILE * 8);
                }
            }
            case 5 -> {
                if (Battle.turn % 2 == Battle.turnOrder) {
                    gc.fillText("Foe " + Battle.opponent.pokemon[0] + "'s " + statNames[Battle.statChanged - 3], GameMap.TILE, GameMap.TILE * 8);
                    gc.fillText("was lowered!", GameMap.TILE, GameMap.TILE * 9);
                } else {
                    gc.fillText( Battle.player.pokemon[0] + "'s " + statNames[Battle.statChanged - 3], GameMap.TILE, GameMap.TILE * 8);
                    gc.fillText( "was lowered!", GameMap.TILE, GameMap.TILE * 9);
                }
            }
            case 6 -> {
                gc.fillText("A critical hit!", GameMap.TILE, GameMap.TILE * 8);
            }
            default -> {}
        }
    }

    public static boolean verifyMoveSlot(int x, int y) {
        return (battleState == 0 || !battleMoves[selectY + y][selectX + x].equals("------"));
    }

    public static void getPlayerMoves() {
        battleMoves[0][0] = Battle.player.moves[0][0];
        battleMoves[0][1] = Battle.player.moves[0][1];
        battleMoves[1][0] = Battle.player.moves[0][2];
        battleMoves[1][1] = Battle.player.moves[0][3];
    }

    public static String getPlayerMove() {
        return playerMove;
    }

    public static void setPlayerMove() {
        playerMove = battleMoves[selectY][selectX];
    }
}