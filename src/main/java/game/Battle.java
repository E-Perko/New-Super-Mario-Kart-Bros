package game;

public class Battle {

    static GameData player = new GameData();
    static GameData opponent = new GameData();

    public static String moveEffect;
    public static int effectChance;
    public static int basePower;
    public static String moveType;
    public static int moveAccuracy;
    public static int powerPoints;
    public static int movePriority;
    public static String moveCategory;

    public static int turn = 1;
    public static int turnOrder = 2;
    public static String currentOwner;

    public static int[] pBattleStats = new int[8];
    public static int[] eBattleStats = new int[8];
    // {Level, Current HP, Max HP, Attack, Defense, SAttack, SDefense, Speed}

    public static int[] pStatMods = new int[7];
    public static int[] eStatMods = new int[7];
    public static final double[] statModList = {2/8.0, 2/7.0, 2/6.0, 2/5.0, 2/4.0, 2/3.0, 2/2.0, 3/2.0, 4/2.0, 5/2.0, 6/2.0, 7/2.0, 8/2.0};
    // {Attack, Defense, SAttack, SDefense, Speed, Accuracy, Evasion} Each stat ranges from -6 to 6

    public double hpAnimP = 0;
    public double hpAnimE = 0;
    // Used to animate HP bars

    public static double critical = 1;
    // The standard crit rate is 1/16, multiplying damage by 1.5

    public static double typeEffect;
    public static boolean miss = false;

    public static int damage;


    public static final String[] types = {"None", "Normal", "Fire", "Water", "Electric", "Grass", "Ice", "Fighting", "Poison", "Ground", "Flying", "Psychic", "Bug", "Rock", "Ghost", "Dragon", "Dark", "Steel", "Fairy"};
    public static final double[][] typeChart = {
            {1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1  },
            {1,  1,  1,  1,  1,  1,  1,  2,  1,  1,  1,  1,  1,  1,  0,  1,  1,  1,  1  },
            {1,  1,  0.5,2,  1,  0.5,0.5,1,  1,  2,  1,  1,  0.5,2,  1,  1,  1,  0.5,0.5},
            {1,  1,  0.5,0.5,2,  2,  0.5,1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  0.5,1  },
            {1,  1,  1,  1,  0.5,1,  1,  1,  1,  2,  0.5,1,  1,  1,  1,  1,  1,  0.5,1  },
            {1,  1,  2,  0.5,0.5,0.5,2,  1,  2,  0.5,2,  1,  2,  1,  1,  1,  1,  1,  1  },
            {1,  1,  2,  1,  1,  1,  0.5,2,  1,  1,  1,  1,  1,  2,  1,  1,  1,  2,  1  },
            {1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  2,  0.5,0.5,1,  1,  0.5,1,  2  },
            {1,  1,  1,  1,  1,  0.5,1,  0.5,0.5,2,  1,  2,  0.5,1,  1,  1,  1,  1,  0.5},
            {1,  1,  1,  2,  0,  2,  2,  1,  0.5,1,  1,  1,  1,  0.5,1,  1,  1,  1,  1  },
            {1,  1,  1,  1,  2,  0.5,2,  0.5,1,  0,  1,  1,  0.5,2,  1,  1,  1,  1,  1  },
            {1,  1,  1,  1,  1,  1,  1,  0.5,1,  1,  1,  0.5,2,  1,  2,  1,  2,  1,  1  },
            {1,  1,  2,  1,  1,  0.5,1,  0.5,1,  0.5,2,  1,  1,  2,  1,  1,  1,  1,  1  },
            {1,  0.5,0.5,2,  1,  2,  1,  2,  0.5,2,  0.5,1,  1,  1,  1,  1,  1,  2,  1  },
            {1,  0,  1,  1,  1,  1,  1,  0,  0.5,1,  1,  1,  0.5,1,  2,  1,  2,  1,  1  },
            {1,  1,  0.5,0.5,0.5,0.5,2,  1,  1,  1,  1,  1,  1,  1,  1,  2,  1,  1,  2  },
            {1,  1,  1,  1,  1,  1,  1,  2,  1,  1,  1,  0,  2,  0,  0.5,1,  0.5,1,  2  },
            {1,  0.5,2,  1,  1,  0.5,0.5,2,  0,  2,  0.5,0.5,0.5,0.5,1,  0.5,1,  0.5,0.5},
            {1,  1,  1,  1,  1,  1,  1,  0.5,2,  1,  1,  1,  0.5,1,  1,  0,  0.5,2,  1  }
    };
    //None, Normal, Fire, Water, Electric, Grass, Ice, Fighting, Poison, Ground, Flying, Psychic, Bug, Rock, Ghost, Dragon, Dark, Steel, Fairy
    //Columns represent defensive interactions and rows represent offensive interactions.
    //2 represents weaknesses/super effective hits, 0.5 represents resistances/resisted hits, 1 represents neutral hits, and 0 represents immunities.

    public void setBattleStats() {
        player.generateStats(player.pokemon[0]);
        pBattleStats = player.battleStats;
        opponent.generateStats(opponent.pokemon[0]);
        eBattleStats = opponent.battleStats;
        hpAnimP = pBattleStats[1];
        hpAnimE = eBattleStats[1];
    }

    public static void playBattleTurn() {
        Battle.typeEffect = 1.0;
        opponent.getMoveInfo(opponent.moves[0][0]);
        player.getMoveInfo(GameInterface.getPlayerMove());
        if (pBattleStats[7] > eBattleStats[7] || player.movePriority > opponent.movePriority) {
            turnOrder = 0;
        } else if (pBattleStats[7] < eBattleStats[7] || player.movePriority < opponent.movePriority) {
            turnOrder = 1;
        } else if (turn % 2 == 1) {
            turnOrder = (int)(Math.random() * 2);
        }
        if (turn % 2 == turnOrder) {
            currentOwner = "Opponent";
        } else {
            currentOwner = "Player";
        }
        executeMove();
    }

    public static int calcDamage(int[] atkStats, int[] defStats) {
        typeEffect = typeChart[indexOfString(getTypes(1)[0][0], types)][indexOfString(moveType, types)] * typeChart[indexOfString(getTypes(1)[1][0], types)][indexOfString(moveType, types)];
        if (typeEffect == 0) {
            return 0;
        }
        int physSpec;
        if (moveCategory.equals("Physical")) {
           physSpec = 0;
        } else {
            physSpec = 2;
        }
        critical = 1;
        if ((int) (Math.random() * 16) == 0) {
            critical = 1.5;
        }
        double stab = 1;
        if (moveType.equals(getTypes(0)[0][0]) || moveType.equals(getTypes(0)[1][0])) {
            stab = 1.5;
        }
        double estimate;
        int atkMod;
        int defMod;
        if (currentOwner.equals("Player")) {
            atkMod = pStatMods[3 + physSpec];
            defMod = eStatMods[4 + physSpec];
        } else {
            atkMod = eStatMods[3 + physSpec];
            defMod = pStatMods[4 + physSpec];
        }

        if (critical == 1) {
            estimate = (((atkStats[0] * 2 / 5.0 + 2) * (basePower * atkStats[3 + physSpec] * statModList[atkMod + 6]) / (((double) defStats[4 + physSpec]) * statModList[defMod + 6] * 50.0))) + 2;
        } else {
            estimate = (((atkStats[0] * 2 / 5.0 + 2) * (basePower * atkStats[3 + physSpec] * statModList[Math.max(atkMod, 0) + 6]) / (((double) defStats[4 + physSpec]) * statModList[Math.min(defMod, 0) + 6] * 50.0))) + 2;
        }
        estimate *= critical * stab * typeEffect * ((int) (Math.random() * 16) + 85) / 100.0;
        return Math.max((int) estimate, 1);
    }

    public int battleEnd() {
        if (hpAnimP <= 0) {
            return 2;
        } else if (hpAnimE <= 0) {
            return 1;
        } else {
            return 0;
        }
    }

    public int getTurn() {
        return turn;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public static int[] getPStats() {
        return pBattleStats;
    }

    public static int[] getEStats() {
        return eBattleStats;
    }

    public static void setPStatMods(int stat, int change) {
        pStatMods[stat] += change;
        pStatMods[stat] = Math.clamp(pStatMods[stat], -6, 6);
    }

    public static void setEStatMods(int stat, int change) {
        eStatMods[stat] += change;
        eStatMods[stat] = Math.clamp(eStatMods[stat], -6, 6);
    }

    public void resetBattle() {
        pBattleStats = player.stats;
        eBattleStats = opponent.stats;
        for (int i = 0; i < pStatMods.length; i++) {
            pStatMods[i] = 0;
            eStatMods[i] = 0;
        }
        turn = 1;
    }

    public static void executeMove() {
        setPersonBattleVars();
        if (currentOwner.equals("Opponent")) {
            if ((int) (Math.random() * 100) < opponent.moveAccuracy) {
                if (!opponent.moveCategory.equals("Status")) {
                    damage = calcDamage(eBattleStats, pBattleStats);
                    pBattleStats[1] -= damage;
                }
                GameData.computeEffect("Opponent");
            } else {
                miss = true;
            }
        } else {
            if ((int) (Math.random() * 100) < player.moveAccuracy) {
                if (!player.moveCategory.equals("Status")) {
                    damage = calcDamage(pBattleStats, eBattleStats);
                    eBattleStats[1] -= damage;
                }
                GameData.computeEffect("Player");

            } else {
                miss = true;
            }
        }
        turn++;
    }

    public static int statChanged;

    public static void setStatChanges(String trainer, int stat, int change) {
       if (trainer.equals("Player")) {
           setPStatMods(stat, change);
       } else {
           setEStatMods(stat, change);
       }
       statChanged = stat;
    }

    public static String[][] getTypes(int trainer) {
        if ((turn % 2 == turnOrder && trainer == 0) || (turn % 2 == 1 - turnOrder && trainer == 1)) {
            return opponent.types;
        } else {
            return player.types;
        }
    }

    public static int indexOfString(String s, String[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(s)) {
                return i;
            }
        }
        return -1;
    }

    public static void heal(String trainer) {
        if (trainer.equals("Player")) {
            Battle.damage = -1 * Battle.getPStats()[2] / 2;
            pBattleStats[1] = Math.min(pBattleStats[1] - damage, pBattleStats[2]);
        } else {
            Battle.damage = -1 * Battle.getEStats()[2] / 2;
            eBattleStats[1] = Math.min(eBattleStats[1] - damage, eBattleStats[2]);
        }
    }

    public static void setPersonBattleVars() {
        if (currentOwner.equals("Player")) {
            moveEffect = player.moveEffect;
            effectChance = player.effectChance;
            basePower = player.basePower;
            moveType = player.moveType;
            moveAccuracy = player.moveAccuracy;
            powerPoints = player.powerPoints;
            movePriority = player.movePriority;
            moveCategory = player.moveCategory;
        } else {
            moveEffect = opponent.moveEffect;
            effectChance = opponent.effectChance;
            basePower = opponent.basePower;
            moveType = opponent.moveType;
            moveAccuracy = opponent.moveAccuracy;
            powerPoints = opponent.powerPoints;
            movePriority = opponent.movePriority;
            moveCategory = opponent.moveCategory;
        }
    }

//    public GameData getPlayerData(String trainer) {
//        if (trainer.equals("Player")) {
//            return player;
//        } else {
//            return opponent;
//        }
//    }
}