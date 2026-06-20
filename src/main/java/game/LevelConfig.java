package game;

/**
 * LevelConfig holds all the tunable numbers that change from level to level.
 *
 * The game loop calls LevelConfig.forLevel(level) once at the start of each
 * level and then reads the four fields below.  Your job is to edit forLevel()
 * so the game gets harder as the level number rises.
 *
 * -----------------------------------------------------------------------
 * FIELD GUIDE — what each field does and how to tune it
 * -----------------------------------------------------------------------
 *
 * ghostSpeedMultiplier
 *   Multiplied against every ghost's base movement speed.
 *   1.0  = normal speed (good for level 1)
 *   1.5  = 50 % faster  (challenging)
 *   2.0  = double speed  (very hard — use with care)
 *   Tip: small increases feel big in practice; try 0.05–0.10 per level.
 *
 * frightenDuration
 *   How many seconds ghosts stay blue after the player eats a power pellet.
 *   6.0  = generous (easy to chase down ghosts)
 *   2.0  = tight window (hard to eat them before they recover)
 *   0.0  = power pellets give no blue time at all (extreme)
 *   Tip: reduce by ~0.5 s per level; clamp to 0 so it never goes negative.
 *
 * spawnDelay
 *   Seconds between each ghost leaving the ghost house at level start.
 *   3.0  = ghosts trickle out slowly (easy opening)
 *   1.0  = ghosts pour out quickly (less breathing room)
 *   0.0  = all ghosts start outside immediately
 *   Tip: reducing this makes the very start of a level more dangerous.
 *
 * bonusThreshold
 *   The player must eat this many regular dots before a BonusItem appears.
 *   70   = bonus item appears roughly mid-level
 *   30   = appears early (more bonus chances)
 *   120  = appears late or possibly never on small boards
 *   Tip: keep it between 30 and 120 for a typical Pac-Man board.
 * -----------------------------------------------------------------------
 */
public class LevelConfig {

    // ------------------------------------------------------------------ //
    //  Fields — public and final so the game loop can read them directly  //
    //  but nothing can accidentally change them mid-level.                //
    // ------------------------------------------------------------------ //

    public final double ghostSpeedMultiplier;
    public final double frightenDuration;
    public final double spawnDelay;
    public final int    bonusThreshold;

    // ------------------------------------------------------------------ //
    //  Constructor                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Creates a LevelConfig with the given values.
     * Use the static factory forLevel() instead of calling this directly
     * unless you are building a custom config for testing.
     */
    public LevelConfig(double ghostSpeedMultiplier,
                       double frightenDuration,
                       double spawnDelay,
                       int    bonusThreshold) {
        this.ghostSpeedMultiplier = ghostSpeedMultiplier;
        this.frightenDuration     = frightenDuration;
        this.spawnDelay           = spawnDelay;
        this.bonusThreshold       = bonusThreshold;
    }

    // ------------------------------------------------------------------ //
    //  Static factory — THIS IS WHERE YOU ADD YOUR LOGIC                  //
    // ------------------------------------------------------------------ //

    /**
     * Returns a LevelConfig tuned for the given level number.
     *
     * level 1 is the first level; there is no level 0.
     *
     * TODO (students): Adjust the formulas below so the game feels fair on
     * level 1 and progressively harder as the level number climbs.  You can
     * use if-statements, Math.min / Math.max, or any approach you like —
     * just make sure the values stay in sensible ranges.
     */
    public static LevelConfig forLevel(int level) {

        // --- ghost speed ---------------------------------------------------
        // Starts at 1.0 and increases by 0.08 each level.
        // Capped at 2.0 so ghosts never become impossible to dodge.
        double speed = (GameMap.TILE / 40.0) + (level - 1) * 0.08;
        speed = Math.min(speed, GameMap.TILE / 20.0);

        // --- frighten duration ---------------------------------------------
        // Starts at 6 seconds and drops by 0.4 s each level.
        // Clamped to 0 so it never goes negative.
        double frighten = 6.0 - (level - 1) * 0.4;
        frighten = Math.max(frighten, 0.0);

        // --- spawn delay ---------------------------------------------------
        // Starts at 3 seconds and shrinks by 0.2 s each level.
        // Clamped to 0 so ghosts never wait a negative amount of time.
        double spawn = 3.0 - (level - 1) * 0.2;
        spawn = Math.max(spawn, 0.0);

        // --- bonus threshold -----------------------------------------------
        // Fixed at 70 dots for every level.
        // Change this if you want the bonus to appear earlier or later.
        int bonus = 75;

        return new LevelConfig(speed, frighten, spawn, bonus);
    }
}
