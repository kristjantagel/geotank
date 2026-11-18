package ee.taltech.wiseones.geotank.server.domain.model.experience;

import ee.taltech.wiseones.geotank.shared.util.LevelUtil;
import lombok.Getter;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_LEVEL;

/**
 * Manages player XP and leveling.
 * XP thresholds are calculated using the shared LevelUtil formula.
 */
@Getter
public class XpManager {
    private int xp;
    private int level = 1;

    public XpManager() {
        this.xp = 0;
    }

    public void reset() {
        xp = 0;
        level = 1;
    }

    /**
     * Add XP and check for level ups. Returns array of newly reached levels or null.
     */
    public int[] addXp(int amount) {
        xp += amount;
        java.util.List<Integer> newLevels = new java.util.ArrayList<>();

        while (level < MAX_LEVEL && xp >= LevelUtil.xpForLevel(level)) {
            level++;
            newLevels.add(level);
        }

        return newLevels.isEmpty() ? null : newLevels.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Get skill points for a specific level using the shared LevelUtil formula.
     */
    public static int getSkillPointsForLevel(int lvl) {
        return LevelUtil.getSkillPointsForLevel(lvl);
    }
}
