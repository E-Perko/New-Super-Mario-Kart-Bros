package game;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PelletPursuitDemo extends Application {

    // ── Customize your game ──────────────────────────────────────────────────
    // Change any of these to make your submission feel like your own.
    private static final String GAME_TITLE        = "Pokémon CS Academy";
    private static final String GAME_SUBTITLE     = "Two franchises in one game?!?";
    private static final String MSG_READY         = "GET READY!";
    private static final String MSG_DEAD          = "OUCH!";
    private static final String MSG_WIN           = "YOU WIN!";
    private static final String LEADERBOARD_TITLE = "-- HIGH SCORES --";
    private static final Color  HUD_COLOR         = Color.web("#111111");
    private static final Color  HUD_TEXT          = Color.WHITE;
    private static final int    STARTING_LIVES    = 3;
    private static final Path   SCORES_FILE       = Path.of("scores.txt");
    // ─────────────────────────────────────────────────────────────────────────

    // --- Layout ---
    private static final int HUD_HEIGHT = GameMap.TILE;

    // Canvas and stage stored as fields so startLevel() can resize them for variable-size maps
    private Canvas canvas;
    private Stage  primaryStage;

    // --- Game state ---
    private enum State { READY, GET_READY, PLAYING, DEAD_PAUSE, LEVEL_CLEAR, GAME_OVER, WIN }

    private GameMap     map;
    private Player      player;
    private List<Ghost> ghosts;

    private int   score  = 0;
    private int   lives  = STARTING_LIVES;
    private int   level  = 1;
    private State state  = State.READY;
    private Ghost killerGhost = null;
    private double pauseTimer = 0;
    private static final double DEAD_PAUSE = 1.5;

    // --- Level config (difficulty scaling) ---
    private LevelConfig config = LevelConfig.forLevel(1);

    // --- Bonus items ---
    private final List<BonusItem> bonusItems = new ArrayList<>();
    private int dotsEaten = 0;

    // --- Ghost spawn stagger ---
    private double spawnTimer   = 0;
    private int    nextToSpawn  = 0;

    // --- Ghost combo scoring (200 → 400 → 800 → 1600 per power pellet) ---
    private int ghostsEatenThisPellet = 0;

    // --- Extra life ---
    private static final int EXTRA_LIFE_THRESHOLD = 2_500;
    private boolean extraLifeAwarded = false;

    // --- Score flashes (points that float up when a ghost is eaten) ---
    private final List<ScoreFlash> scoreFlashes = new ArrayList<>();

    // --- Level clear flash ---
    private static final double LEVEL_CLEAR_DURATION = 2.0;
    private static final int    MAX_LEVEL             = 5;

    // --- HUD notification (e.g. "+1 UP!") ---
    private String hudMessage     = null;
    private double hudMessageTimer = 0;

    // --- Persistent high scores ---

    private final AudioManager audio = new AudioManager();
    private static final Battle battle = new Battle();
    private final GraphicsManager graphics = new GraphicsManager();

    private boolean paused            = false;
    private boolean freeze            = false;
    public boolean inBattle          = false;
    private Ghost battleGhost = null;

    public String battleMusic;
    public String[] randomMusic = {"gym", "eliteFour", "champion", "gym2", "battle"};
    public String currentBattle;
    public String[] randomBattle = {"Rival_1_Charmander", "Rival_1_Squirtle", "Rival_1_Bulbasaur", "Cynthia_Spiritomb", "Cynthia_Roserade", "Cynthia_Togekiss", "Cynthia_Milotic", "Cynthia_Lucario", "Cynthia_Garchomp"};
    public String[] playerTeams = {"Rival_1_Charmander", "Rival_1_Squirtle", "Rival_1_Bulbasaur", "Player_Zoroark", "Player_Polteageist", "Player_Avalugg", "Player_Appletun", "Player_Milotic"};

    public boolean criticalMusic = false;
    public boolean betweenTurns = false;

    public static boolean[] battleMessageValues = new boolean[4];
    //Move Missed, Type Effectiveness, Stat Lowered, Critical Hit
    //public ArrayList<Integer> currentBattleMessages = new ArrayList<>();

    public static boolean animHP;
    public static double battlePauseTimer = 0.0;

    private ScoreTree scoreTree = new ScoreTree();
    private long lastNano = -1;

    // Convenience — canvas height tracks HUD + current map height
    public int canvasH() { return map.height + HUD_HEIGHT; }
    StackPane root;

    /**
     * Return the Tile[][] layout for a given level number.
     * Add new cases here to give each level a different maze.
     * All layouts must satisfy the rules in GameMap's constructor Javadoc.
     */
    protected GameMap.Tile[][] getLayout(int lvl) {
        return GameMap.DEFAULT_LAYOUT;
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        initGame();

        canvas = new Canvas(map.width, canvasH());
        GraphicsContext gc = canvas.getGraphicsContext2D();

        root = new StackPane(canvas);

        Scene scene = new Scene(root, GameMap.TILE * 15, (GameMap.TILE * 10) + HUD_HEIGHT, Color.BLACK);

        //graphics.displayImg("battle_background", 15, -20, 0, 0, root);
        graphics.displayImg("bulbasaur_back", 4, -10, 3.875, 1, root);
        graphics.displayImg("squirtle_front", 4, -10, 0.875, 2, root);

        scene.setOnKeyPressed(e -> handleKey(e.getCode()));

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (lastNano < 0) ? 0 : (now - lastNano) / 1_000_000_000.0;
                lastNano = now;
                dt = Math.min(dt, 0.05);
                update(dt);
                render(gc);
            }
        }.start();

        stage.setTitle(GAME_TITLE);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        stage.centerOnScreen();
        canvas.requestFocus();
        audio.playSong("town");
    }

    private void initGame() {
        map       = new GameMap(getLayout(1));
        player    = new Player(map);
        // Shadow is the worked example — study it before implementing the others.
        // Add each ghost here after you finish its chooseTarget() in Phase 2:
        //   new Patrol(map), new Shy(map), new Ambush(map)
        ghosts    = new ArrayList<>(List.of(new Shadow(map), new Patrol(map), new Shy(map), new Ambush(map)));
        scoreTree = new ScoreTree();
        scoreTree.loadFromFile(SCORES_FILE);
        state  = State.READY;
    }

    private void startLevel() {
        config = LevelConfig.forLevel(level);
        map    = new GameMap(getLayout(level));
        player = new Player(map);
        // Add each ghost here after you finish its chooseTarget() in Phase 2:
        //   new Patrol(map), new Shy(map), new Ambush(map)
        ghosts = new ArrayList<>(List.of(new Shadow(map), new Patrol(map), new Shy(map), new Ambush(map)));

        // Resize the window only when the new layout has different pixel dimensions
        if (canvas != null &&
                ((int) canvas.getWidth() != map.width || (int) canvas.getHeight() != canvasH())) {
            canvas.setWidth(map.width);
            canvas.setHeight(canvasH());
            primaryStage.sizeToScene();
            primaryStage.centerOnScreen();
        }

        for (Ghost g : ghosts) {
            g.applySpeedMultiplier(config.ghostSpeedMultiplier);
        }
        // Stagger ghost spawns — ghosts 1-3 start inactive and are released one per spawnDelay
        nextToSpawn = 1;
        spawnTimer  = 0;
        if (config.spawnDelay > 0) {
            for (int i = 1; i < ghosts.size(); i++) ghosts.get(i).setActive(false);
        }
        dotsEaten = 0;
        bonusItems.clear();
        scoreFlashes.clear();
        ghostsEatenThisPellet = 0;
        paused            = false;
        pauseTimer = 2.0;
        state = State.GET_READY;
    }

    private void handleKey(KeyCode key) {
        if (key == KeyCode.SPACE && state == State.PLAYING && inBattle && !animHP && battlePauseTimer <= 0 && battle.getTurn() % 2 == 1) {
            if (GameInterface.battleState == 1) {
                GameInterface.setPlayerMove();
                playTurn();
            } else if (GameInterface.selectX == 0 && GameInterface.selectY == 0) {
                GameInterface.battleState = 1;
            }
        }
        if (key == KeyCode.X && state == State.PLAYING && inBattle && !animHP && battle.getTurn() % 2 == 1) {
            GameInterface.battleState = 0;
            GameInterface.selectX = 0;
            GameInterface.selectY = 0;
        }
        if (key == KeyCode.ENTER) {
            if (state == State.READY || state == State.WIN) {
                level = 1; score = 0; lives = STARTING_LIVES; extraLifeAwarded = false; startLevel();
            } else if (state == State.GAME_OVER) {
                level = 1; score = 0; lives = STARTING_LIVES; extraLifeAwarded = false; initGame(); startLevel();
            } else if (state == State.PLAYING && !freeze) {
                paused = !paused;
            }
        }
        if (state == State.PLAYING || state == State.GET_READY) player.handleKey(key);
        if (inBattle) {
            if (key == KeyCode.RIGHT && GameInterface.selectX == 0 && GameInterface.verifyMoveSlot(1, 0)) {
                GameInterface.selectX = 1;
            }
            if (key == KeyCode.LEFT && GameInterface.selectX == 1 && GameInterface.verifyMoveSlot(-1, 0)) {
                GameInterface.selectX = 0;
            }
            if (key == KeyCode.DOWN && GameInterface.selectY == 0 && GameInterface.verifyMoveSlot(0, 1)) {
                GameInterface.selectY = 1;
            }
            if (key == KeyCode.UP && GameInterface.selectY == 1 && GameInterface.verifyMoveSlot(0, -1)) {
                GameInterface.selectY = 0;
            }
        }
    }

    private void update(double dt) {
        if (inBattle) {
            if (animHP && battlePauseTimer <= 0) {
                battlePauseTimer = 0;
                if (battle.hpAnimP > Battle.getPStats()[1] + (dt * Battle.getPStats()[2] / 2) && Battle.damage > 0) {
                    battle.hpAnimP -= dt * Math.min(Battle.damage * 2.5, Battle.getEStats()[2] / 2.0);
                } else if (battle.hpAnimP < Battle.getPStats()[1] - (dt * Battle.getPStats()[2] / 2) && Battle.damage < 0) {
                    battle.hpAnimP -= dt * Math.max(Battle.damage * 2.5, Battle.getPStats()[2] / -2.0);
                } else {
                    battle.hpAnimP = Battle.getPStats()[1];
                }
                if (battle.hpAnimE > Battle.getEStats()[1] + (dt * Battle.getEStats()[2] / 2) && Battle.damage > 0) {
                    battle.hpAnimE -= dt * Math.min(Battle.damage * 2.5, Battle.getEStats()[2] / 2.0);
                } else if (battle.hpAnimE < Battle.getEStats()[1] - (dt * Battle.getEStats()[2] / 2) && Battle.damage < 0) {
                    battle.hpAnimE -= dt * Math.max(Battle.damage * 2.5, Battle.getEStats()[2] / -2.0);
                } else {
                    battle.hpAnimE = Battle.getEStats()[1];
                }
                if ((Battle.damage > 0 && battle.hpAnimP <= Math.max(Battle.getPStats()[1], 0) && battle.hpAnimE <= Math.max(Battle.getEStats()[1], 0)) || (Battle.damage < 0 && battle.hpAnimP >= Math.min(Battle.getPStats()[1], Battle.getPStats()[2]) && battle.hpAnimE >= Math.min(Battle.getEStats()[1], Battle.getEStats()[2])) || Battle.damage == 0) {
                    battle.hpAnimP = Battle.getPStats()[1];
                    battle.hpAnimE = Battle.getEStats()[1];
                    animHP = false;
                    betweenTurns = true;
                    battlePauseTimer = 0.5;
                }
            }

            if (battle.hpAnimP / Battle.getPStats()[2] < 0.25 && !criticalMusic && !animHP && Battle.getPStats()[1] > 0) {
                audio.playSong("critical");
                criticalMusic = true;
            }
            if (battle.hpAnimP / Battle.getPStats()[2] > 0.25 && criticalMusic && !animHP) {
                audio.playSong(battleMusic);
                criticalMusic = false;
            }

            battleMessageValues[0] = Battle.miss;
            battleMessageValues[2] = GameData.loweredStats;

            for (int i = 0; i < battleMessageValues.length; i++) {
                if (battleMessageValues[i] && !animHP && battlePauseTimer <= 0) {
                    battleMessageValues[i] = false;
                    GameInterface.battleState = i + 3;
                    battlePauseTimer = 1.0;
                    if (i == 0) {
                       Battle.miss = false;
                    }
                    if (i == 2) {
                        GameData.loweredStats = false;
                    }
                    if (i == 3) {
                        Battle.critical = 1;
                    }
                }
            }

            if (battle.battleEnd() != 0 && Battle.critical == 1 && betweenTurns && battlePauseTimer <= 0 && !battleMessageValues[3] && !battleMessageValues[1]) {
                terminateBattle();
                if (battle.battleEnd() == 1) {
                    win(battleGhost);
                } else {
                    death(battleGhost);
                }
                return;
            }

            if (betweenTurns && battlePauseTimer <= 0 && !animHP) {
                betweenTurns = false;
                if (battle.getTurn() % 2 == 0) {
                    playTurn();
                } else {
                    GameInterface.battleState = 0;
                }
            }

            if (battlePauseTimer > 0) {
                battlePauseTimer -= dt;
            }
        }

        if (paused || freeze) return;
        if (hudMessageTimer > 0) hudMessageTimer -= dt;

        if (state == State.GET_READY) {
            pauseTimer -= dt;
            if (pauseTimer <= 0) {
                state = State.PLAYING;
            }
            return;
        }
        if (state == State.LEVEL_CLEAR) {
            pauseTimer -= dt;
            if (pauseTimer <= 0) {
                if (level >= MAX_LEVEL) {
                    state = State.WIN;
                } else {
                    level++;
                    startLevel();
                }
            }
            return;
        }
        if (state == State.DEAD_PAUSE) {
            pauseTimer -= dt;
            if (pauseTimer <= 0) {
                if (lives <= 0) {
                    scoreTree.insert(score, level);
                    scoreTree.saveToFile(SCORES_FILE);
                    state = State.GAME_OVER;
                } else {
                    killerGhost = null;
                    player.resetPosition();
                    for (Ghost g : ghosts) g.resetPosition();
                    nextToSpawn = 1;
                    spawnTimer  = 0;
                    if (config.spawnDelay > 0) {
                        for (int i = 1; i < ghosts.size(); i++) ghosts.get(i).setActive(false);
                    }
                    scoreFlashes.clear();
                    pauseTimer = 2.0;
                    state = State.GET_READY;
                }
            }
            return;
        }
        if (state != State.PLAYING) return;

        // Release staggered ghosts
        if (nextToSpawn < ghosts.size()) {
            spawnTimer += dt;
            if (spawnTimer >= config.spawnDelay) {
                ghosts.get(nextToSpawn).setActive(true);
                nextToSpawn++;
                spawnTimer = 0;
            }
        }

        player.update(dt, map);

        // Eat dots
        int col = player.col(map), row = player.row(map);
        int earned = map.eatDot(col, row);
        if (earned > 0) {
            score += earned;
            dotsEaten++;
            if (earned == 50) { // power pellet
                audio.playPellet();
                for (Ghost g : ghosts) g.frighten(config.frightenDuration);
                ghostsEatenThisPellet = 0;
            } else {
                audio.playChomp();
            }
            if (!extraLifeAwarded && score >= EXTRA_LIFE_THRESHOLD) {
                lives++;
                extraLifeAwarded = true;
                hudMessage     = "+1 UP!";
                hudMessageTimer = 2.0;
            }
            if (bonusItems.isEmpty() && dotsEaten >= config.bonusThreshold) {
                double bx = map.tileCenterX(map.spawnCol(GameMap.Tile.SPAWN_BONUS)) - GameMap.TILE / 2.0;
                double by = map.tileCenterY(map.spawnRow(GameMap.Tile.SPAWN_BONUS)) - GameMap.TILE / 2.0;
                bonusItems.add(new Cherry(bx, by));
            }
        }

        updateBonusItems(dt);

        // Update ghosts
        for (Ghost g : ghosts) g.update(dt, map, player);

        // Ghost collisions
        for (Ghost g : ghosts) {
            if (!g.isActive() || g.isDead()) continue;
            if (g.collidesWith(player)) {
                if (g.isFrightened()) {
                    int pts = Math.max(200 * (1 << ghostsEatenThisPellet), 1600);
                    score += pts;
                    ghostsEatenThisPellet++;
                    audio.playGhostEaten();
                    scoreFlashes.add(new ScoreFlash(g.centerX(), g.centerY(), pts));
                    g.kill();
                } else {
                    int random = (int) (Math.random() * randomBattle.length);
                    //int random = (int) (Math.random() * 3);
                    //int random = 7;
                    //int random = (int) (Math.random() * (randomBattle.length - 3)) + 3;
                    currentBattle = randomBattle[random];
                    if (random > 2) {
                        Battle.player.generateTeam(playerTeams[(int) (Math.random() * (playerTeams.length - 3)) + 3], "player");
                        //Battle.player.generateTeam(playerTeams[4], "player");
                    } else {
                        Battle.player.generateTeam(currentBattle, "player");
                    }
                    Battle.opponent.generateTeam(currentBattle, "opponent");
                    graphics.displayImg( Battle.player.pokemon[0].toLowerCase() + "_back", 4.5, 1, 2.5, 1, root);
                    graphics.displayImg( Battle.opponent.pokemon[0].toLowerCase() + "_front", 4.5, 8.5, -0.75, 2, root);
                    GameInterface.getPlayerMoves();
                    battle.setBattleStats();
                    freeze = true;
                    inBattle = true;
                    battleMusic = randomMusic[(int) (Math.random() * randomMusic.length)];
                    audio.playSong(battleMusic);
                    battleGhost = g;
                    GameInterface.battleState = 0;
                    return;
                }
            }
        }

        // Tick score flashes
        scoreFlashes.forEach(f -> f.update(dt));
        scoreFlashes.removeIf(f -> f.timer <= 0);

        // Level clear — flash the maze then advance
        if (map.allDotsEaten()) {
            scoreTree.insert(score, level);
            scoreTree.saveToFile(SCORES_FILE);
            pauseTimer = LEVEL_CLEAR_DURATION;
            state = State.LEVEL_CLEAR;
        }
    }

    private void updateBonusItems(double dt) {
        // TODO (Phase 3): Update every bonus item and handle collection and expiry.
        //
        // Step 1 — update each item and award points if the player touches it:
        //   for (BonusItem item : bonusItems) {
        //       item.update(dt, map);
        //       if (item.collidesWith(player)) {
        //           score += item.getPoints();
        //           audio.playBonus();
        //       }
        //   }
        //
        // Step 2 — build a removal list, then remove those items:
        //   List<BonusItem> toRemove = new ArrayList<>();
        //   for (BonusItem item : bonusItems) {
        //       if (item.collidesWith(player) || item.isExpired()) {
        //           toRemove.add(item);
        //       }
        //   }
        //   bonusItems.removeAll(toRemove);
        //
        // Why two loops? Java throws a ConcurrentModificationException if you
        // call bonusItems.remove() inside a for-each loop over bonusItems.
        // Collecting items to remove first and deleting them after is the safe pattern.
    }

    private void render(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, map.width, canvasH());

        // Game area shifted down by HUD_HEIGHT
        gc.save();
        //gc.translate(0, HUD_HEIGHT);
        if (state == State.LEVEL_CLEAR) {
            // Alternate between white and blue walls every 0.25 s
            Color wallColor = ((int)(pauseTimer / 0.25) % 2 == 0)
                ? Color.web("#202090") : Color.web("#f0f0f0");
            map.draw(gc, wallColor);
        } else {
            map.draw(gc);
        }
        player.draw(gc);
        for (Ghost g : ghosts) g.draw(gc);
        for (BonusItem b : bonusItems) b.draw(gc);
        for (ScoreFlash f : scoreFlashes) f.draw(gc);
        gc.restore();

        // HUD
        if (inBattle) {
            drawBattleHUD(gc);
        } else {
            drawHUD(gc);
        }

        // Overlays
        if (state == State.READY) {
            gc.setFill(Color.color(0, 0, 0, 0.30));
            gc.fillRect(0, 0, map.width, canvasH());
            drawCenteredText(gc, GAME_TITLE, (int) (GameMap.TILE * 2 / 3.0), Color.YELLOW, canvasH() / 2.0 - 40);
            drawCenteredText(gc, GAME_SUBTITLE, (int) (GameMap.TILE / 4.0), Color.web("#aaaaaa"), canvasH() / 2.0 - 10);
            drawCenteredText(gc, "PRESS ENTER TO START", (int) (GameMap.TILE / 3.0), HUD_TEXT, canvasH() / 2.0 + 20);
        } else if (state == State.GET_READY) {
            drawCenteredText(gc, MSG_READY, 36, Color.YELLOW, canvasH() / 2.0);
        } else if (state == State.DEAD_PAUSE) {
            drawCenteredText(gc, MSG_DEAD, 36, Color.RED, canvasH() / 2.0 - 10);
            if (killerGhost != null)
                drawCenteredText(gc, "caught by " + killerGhost.getName(), 16, Color.web("#ff8888"), canvasH() / 2.0 + 20);
        } else if (state == State.GAME_OVER) {
            drawGameOver(gc);
        } else if (state == State.WIN) {
            drawCenteredText(gc, MSG_WIN, 40, Color.YELLOW, canvasH() / 2.0 - 40);
            drawCenteredText(gc, "PRESS ENTER TO PLAY AGAIN", 16, Color.web("#666"), GameMap.TILE * 8);
        }

        if (paused) {
            gc.setFill(Color.color(0, 0, 0, 0.55));
            gc.fillRect(0, 0, map.width, canvasH());
            drawCenteredText(gc, "PAUSED", 40, Color.YELLOW, GameMap.TILE * 5);
            drawCenteredText(gc, "PRESS ENTER TO RESUME", 18, Color.WHITE, GameMap.TILE * 6);
        }

        if (inBattle) {
            gc.setFill(Color.web("#c0c0c0"));
            gc.fillRect(0, 0, map.width, map.height);
            gc.setFill(Color.web("#c0f0c0"));
            gc.fillRect(0, GameMap.TILE * 7, map.width, GameMap.TILE * 3);
            gc.setFill(Color.DARKSLATEGRAY);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFont(Font.font("Monospace", GameMap.TILE * 2 / 3.0));
            gc.fillText(Battle.opponent.pokemon[0] + " Level " + Battle.opponent.levels[0], GameMap.TILE, GameMap.TILE);
            gc.fillText(Battle.player.pokemon[0] + " Level " + Battle.player.levels[0], GameMap.TILE * 6, GameMap.TILE * 5);
            gc.fillText("Health: " + Math.max(Math.round(battle.hpAnimE), 0) + " / " + Battle.getEStats()[2], GameMap.TILE, GameMap.TILE * 2);
            gc.fillText("Health: " + Math.max(Math.round(battle.hpAnimP), 0) + " / " + Battle.getPStats()[2], GameMap.TILE * 6, GameMap.TILE * 6);
            //graphics.setImgX(0, 0);
            GameInterface.renderBattle(gc);
            gc.fillRect(GameMap.TILE * 4, GameMap.TILE * 2.125, GameMap.TILE * 3, GameMap.TILE / 2.0);
            gc.fillRect(GameMap.TILE * 9, GameMap.TILE * 6.125, GameMap.TILE * 3, GameMap.TILE / 2.0);
            gc.setFill(Color.web(hpColor(battle.hpAnimE / Battle.getEStats()[2])));
            gc.fillRect(GameMap.TILE * 4, GameMap.TILE * 2.25, GameMap.TILE * 3.0 * battle.hpAnimE / Battle.getEStats()[2], GameMap.TILE / 4.0);
            gc.setFill(Color.web(hpColor(battle.hpAnimP / Battle.getPStats()[2])));
            gc.fillRect(GameMap.TILE * 9, GameMap.TILE * 6.25, GameMap.TILE * 3.0 * battle.hpAnimP / Battle.getPStats()[2], GameMap.TILE / 4.0);
        }
    }

    private void drawHUD(GraphicsContext gc) {
        gc.setFill(HUD_COLOR);
        gc.fillRect(0, GameMap.TILE * 10, map.width, HUD_HEIGHT);

        // Lives as small pac-man icons

        for (int i = 0; i < lives; i++) {
            double lx = map.width - (GameMap.TILE * 9 / 2.0) - i * (GameMap.TILE / 1.75);
            gc.setFill(Color.YELLOW);
            gc.fillArc(lx, GameMap.TILE * 10.25, GameMap.TILE / 2.0, GameMap.TILE / 2.0, 30, 300, javafx.scene.shape.ArcType.ROUND);
        }

        // Pause hint
        gc.setFont(Font.font("Monospace", GameMap.TILE / 4.5));
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("USE ARROW KEYS TO MOVE", map.width - GameMap.TILE / 4.0, HUD_HEIGHT + (GameMap.TILE * 9.375));
        gc.fillText("PRESS ENTER TO PAUSE", map.width - GameMap.TILE / 4.0, HUD_HEIGHT + (GameMap.TILE * 9.75));

        gc.setFont(Font.font("Monospace", GameMap.TILE / 3.5));
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("SCORE:  " + score, GameMap.TILE, GameMap.TILE * 10.5);
        gc.fillText("LEVEL " + level, GameMap.TILE * 5.5, GameMap.TILE * 10.5);

        // Transient HUD notification (extra life, etc.)
        if (hudMessageTimer > 0) {
            gc.setFont(Font.font("Monospace", 14));
            gc.setFill(Color.YELLOW);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(hudMessage, map.width / 2.0, 44);
        }
    }

    private void drawBattleHUD(GraphicsContext gc) {
        gc.setFill(Color.web("#a0c0a0"));
        gc.fillRect(0, GameMap.TILE * 10, map.width, HUD_HEIGHT);
        gc.setFont(Font.font("Monospace", GameMap.TILE / 3.5));
        gc.setFill(Color.DARKSLATEGRAY);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Use arrow keys\nto move cursor", GameMap.TILE * 2, HUD_HEIGHT + (GameMap.TILE * 9.375));
        gc.fillText("Press space\nto advance", GameMap.TILE * 6.5, HUD_HEIGHT + (GameMap.TILE * 9.375));
        gc.fillText("Press X to\ngo back", GameMap.TILE * 10.5, HUD_HEIGHT + (GameMap.TILE * 9.375));
    }

    private void drawGameOver(GraphicsContext gc) {
        gc.setFill(Color.color(0, 0, 0, 0.75));
        gc.fillRect(0, 0, map.width, canvasH());

        drawCenteredText(gc, "GAME OVER", 40, Color.RED, GameMap.TILE * 3);
        drawCenteredText(gc, "SCORE: " + score, 26, Color.WHITE, GameMap.TILE * 4);

        if (!scoreTree.isEmpty()) {
            drawCenteredText(gc, LEADERBOARD_TITLE, 18, Color.YELLOW, 260);
            List<ScoreNode> top = scoreTree.getTopScores(5);
            for (int i = 0; i < top.size(); i++) {
                ScoreNode n = top.get(i);
                String line = (i + 1) + ".  " + n.score + "  (level " + n.level + ")";
                drawCenteredText(gc, line, 16, Color.web("#aaa"), 290 + i * 24);
            }
        }

        drawCenteredText(gc, "PRESS ENTER TO PLAY AGAIN", 18, Color.WHITE, GameMap.TILE * 8);
    }

    private void drawCenteredText(GraphicsContext gc, String text, int size, Color color, double y) {
        gc.setFont(Font.font("Monospace", size));
        gc.setFill(color);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, map.width / 2.0, y);
    }

    // -----------------------------------------------------------------------
    // Score flash — points that float upward and fade when a ghost is eaten
    // -----------------------------------------------------------------------

    private static final class ScoreFlash {
        double x, y;
        final int value;
        double timer = 1.0; // seconds until gone

        ScoreFlash(double x, double y, int value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }

        void update(double dt) {
            timer -= dt;
            y -= dt * 24; // float upward 24 px/s
        }

        void draw(GraphicsContext gc) {
            gc.save();
            gc.setGlobalAlpha(Math.max(0, timer)); // fades 1→0 over 1 second
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Monospace", 14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.valueOf(value), x, y);
            gc.restore();
        }
    }

    public void death(Ghost g) {
        audio.playSong("town");
        lives--;
        killerGhost = g;
        audio.playDeath();
        state      = State.DEAD_PAUSE;
        pauseTimer = DEAD_PAUSE;
        battle.resetBattle();
    }

    public void win(Ghost g) {
        audio.playSong("town");
        int pts = 200 * (1 << ghostsEatenThisPellet);
        score += pts;
        ghostsEatenThisPellet++;
        audio.playGhostEaten();
        scoreFlashes.add(new ScoreFlash(g.centerX(), g.centerY(), pts));
        g.kill();
        battle.resetBattle();
    }

    public void terminateBattle() {
        inBattle = false;
        freeze = false;
        criticalMusic = false;
        //graphics.setImgX(0, GameMap.TILE * -20);
        graphics.deleteImage(1);
        graphics.deleteImage(2);
        GameInterface.battleState = 0;
        GameInterface.selectX = 0;
        GameInterface.selectY = 0;
    }

    public String hpColor(double hpRatio) {
        if (hpRatio < 0.25) {
            return "#e3110e";
        } else if (hpRatio < 0.5) {
            return "#f7ed20";
        } else {
            return "#8bd95b";
        }
    }

    public static void playTurn() {
        GameInterface.battleState = 2;
        battlePauseTimer = 0.5;
        Battle.playBattleTurn();
        if (Battle.critical > 1) {
            battleMessageValues[3] = true;
        }
        if (Battle.typeEffect != 1.0) {
            battleMessageValues[1] = true;
        }
        animHP = true;
        GameInterface.selectX = 0;
        GameInterface.selectY = 0;
    }

    public static void main(String[] args) { launch(args); }
}