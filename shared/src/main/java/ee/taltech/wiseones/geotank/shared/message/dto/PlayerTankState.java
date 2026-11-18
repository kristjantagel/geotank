package ee.taltech.wiseones.geotank.shared.message.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTankState {
    private int tankId;

    private float x;
    private float y;
    private float direction;
    private int lives;
    private int maxLives;
    private boolean alive;

    // Stats for HUD (XP bar, leaderboard)
    private int xp;
    private int level;
    private int score;
    private int skillPoints;

    private String name;

    // Visual stats — let the client render the right number of barrels at the right spread.
    private int cannonCount;
    private float spreadAngle;

    @Override
    public String toString() {
        return String.format("PlayerTankState(%d, %f, %f, %f, %d, alive=%b, lvl=%d, xp=%d, score=%d, name=%s)",
                tankId, x, y, direction, lives, alive, level, xp, score, name);
    }
}
