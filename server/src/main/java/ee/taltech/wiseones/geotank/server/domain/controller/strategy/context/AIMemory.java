package ee.taltech.wiseones.geotank.server.domain.controller.strategy.context;

import ee.taltech.wiseones.geotank.server.domain.model.Player;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * AI memory system for tracking state and history.
 * Persists across multiple decision cycles to support learning and prediction.
 */
@Getter
@Setter
public class AIMemory {
    private final Map<Integer, EnemyProfile> enemyProfiles = new HashMap<>();
    private Player currentThreat;
    private long lastMajorDecisionTime = 0;
    private float lastHealthPercentage = 1.0f;

    public void updateEnemyProfile(Player enemy, float x, float y, float vx, float vy) {
        int id = enemy.getId();
        EnemyProfile profile = enemyProfiles.computeIfAbsent(id,
                k -> new EnemyProfile(enemy.getId(), enemy.getName()));
        profile.updatePosition(x, y, vx, vy);
    }

    public EnemyProfile getOrCreateEnemyProfile(Player enemy) {
        return enemyProfiles.computeIfAbsent(enemy.getId(),
                k -> new EnemyProfile(enemy.getId(), enemy.getName()));
    }

    public void removeEnemyProfile(int playerId) {
        enemyProfiles.remove(playerId);
    }

    /**
     * Profile of an enemy player tracked over time
     */
    @Getter
    public static class EnemyProfile {
        public static final float DEFAULT_AGGRESSIVENESS = 0.5f;
        private final int playerId;
        private final String playerName;

        private float lastX;
        private float lastY;
        private float velocityX;
        private float velocityY;
        private long lastSeenTime;
        private final int threatsAccounted;
        private float aggressiveness; // 0.0 - 1.0

        public EnemyProfile(int playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.lastX = 0;
            this.lastY = 0;
            this.velocityX = 0;
            this.velocityY = 0;
            this.lastSeenTime = 0;
            this.threatsAccounted = 0;
            this.aggressiveness = DEFAULT_AGGRESSIVENESS;
        }

        public void updatePosition(float x, float y, float vx, float vy) {
            this.lastX = x;
            this.lastY = y;
            this.velocityX = vx;
            this.velocityY = vy;
            this.lastSeenTime = System.currentTimeMillis();
        }

        public void decayAggressiveness() {
            if (aggressiveness > DEFAULT_AGGRESSIVENESS) {
                this.aggressiveness = Math.max(DEFAULT_AGGRESSIVENESS, aggressiveness - 0.01f);
            }
        }
    }
}
