package ee.taltech.wiseones.geotank.shared.message;

import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StatsUpdateMessage {
    private int playerId;
    private int level;
    private int xp;
    private int score;
    private int skillPoints;
    private int[] skillLevels; // 8 values, indexed by SkillType ordinal
    private TankClass primaryClass;
    private TankClass secondaryClass;

    public StatsUpdateMessage(int playerId, int level, int xp, int score, int skillPoints,
                              int[] skillLevels, TankClass primaryClass, TankClass secondaryClass) {
        this.playerId = playerId;
        this.level = level;
        this.xp = xp;
        this.score = score;
        this.skillPoints = skillPoints;
        this.skillLevels = skillLevels;
        this.primaryClass = primaryClass;
        this.secondaryClass = secondaryClass;
    }
}
