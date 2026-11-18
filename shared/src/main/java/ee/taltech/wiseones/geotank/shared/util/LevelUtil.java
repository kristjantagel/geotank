package ee.taltech.wiseones.geotank.shared.util;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_LEVEL;

/**
 * Diep.io XP threshold calculations, shared between server and client.
 * Based on the formula: delta(i) = (DIVIDEND/DIVISOR) * BASE^(i-1) * min(MIN_POWER, i)
 */
public final class LevelUtil {
    // XP formula constants
    public static final double DIVIDEND = 40.0;
    public static final double DIVISOR = 9.0;
    public static final double BASE = 1.06;
    public static final int MIN_POWER = 31;
    public static final int LINEAR_THRESHOLD = 28;  // Levels 2-28 have 1 point per level

    private static final int[] XP_THRESHOLDS;

    static {
        XP_THRESHOLDS = new int[MAX_LEVEL + 1];
        XP_THRESHOLDS[0] = 0;
        double cumulative = 0;
        for (int i = 1; i <= MAX_LEVEL; i++) {
            double delta = (DIVIDEND / DIVISOR) * Math.pow(BASE, (double) i - 1) * Math.min(MIN_POWER, i);
            cumulative += delta;
            XP_THRESHOLDS[i] = (int) Math.round(cumulative);
        }
    }

    private LevelUtil() {
    }

    /** Total XP needed to reach a given level. */
    public static int xpForLevel(int level) {
        if (level <= 0) return 0;
        if (level > MAX_LEVEL) return XP_THRESHOLDS[MAX_LEVEL];
        return XP_THRESHOLDS[level];
    }

    /** XP threshold for the level the player is currently in (start of current level). */
    public static int xpForCurrentLevel(int level) {
        if (level <= 1) return 0;
        return XP_THRESHOLDS[level - 1];
    }

    /** XP threshold for the next level (end of current level). */
    public static int xpForNextLevel(int level) {
        if (level >= MAX_LEVEL) return XP_THRESHOLDS[MAX_LEVEL];
        return XP_THRESHOLDS[level];
    }

    /**
     * Get skill points awarded for reaching a specific level.
     * Diep.io distribution:
     * - Levels 2-28: 1 point per level
     * - Level 29: none
     * - Level 30: 1 point
     * - Levels 31-45: 1 point every 3 levels (33, 36, 39, 42, 45)
     */
    public static int getSkillPointsForLevel(int lvl) {
        if (lvl <= 1) return 0;
        if (lvl <= LINEAR_THRESHOLD) return 1;
        if (lvl == LINEAR_THRESHOLD + 1) return 0;
        if (lvl == LINEAR_THRESHOLD + 2) return 1;
        return (lvl % 3 == 0) ? 1 : 0;
    }
}
