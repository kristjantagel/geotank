package ee.taltech.wiseones.geotank.server.domain.controller.strategy.impl;

import ee.taltech.wiseones.geotank.server.domain.controller.strategy.AIStrategy;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIContext;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.util.MovementUtils;
import ee.taltech.wiseones.geotank.server.domain.model.Player;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

public class HealthManagementStrategy implements AIStrategy {
    private static final float LOW_HEALTH_THRESHOLD = 0.4f;
    public static final float THREAT_PER_LEVEL = 0.2f;
    private static final float CRITICAL_HEALTH_THRESHOLD = THREAT_PER_LEVEL;
    private static final float RETREAT_MULTIPLIER = 1.3f;
    public static final float CRITICAL_HEALTH_PRIORITY = 0.96f;
    public static final float LOW_DAMAGE_RATE_PRIORITY = 0.80f;
    public static final float HIGH_DAMAGE_RATE_PRIORITY = 0.90f;
    public static final float HIGH_DAMAGE_RATE_THRESHOLD = 0.05f;
    public static final float STRONGER_ENEMY_RETREAT_MULTIPLIER = 1.15f;
    private long lastHealthChangeTime = 0;
    private float lastRecordedHealth = 1.0f;

    @Override
    public void execute(AIContext context) {
        Player aiPlayer = context.getAiPlayer();
        float healthRatio = (float) aiPlayer.getLives() / (float) aiPlayer.getMaxHp();

        // Track health changes
        long currentTime = System.currentTimeMillis();
        if (Math.abs(healthRatio - lastRecordedHealth) > (float) 1 / 100) {
            lastHealthChangeTime = currentTime;
            lastRecordedHealth = healthRatio;
        }

        // Adjust thresholds based on threat level
        float adjustedLowThreshold = getAdjustedThreshold(context, LOW_HEALTH_THRESHOLD);
        float adjustedCriticalThreshold = getAdjustedThreshold(context, CRITICAL_HEALTH_THRESHOLD);

        // At critical health, always retreat
        if (healthRatio < adjustedCriticalThreshold) {
            executeCriticalRetreat(context);
        } else if (healthRatio < adjustedLowThreshold) {
            executeRetreat(context);
        }
    }

    @Override
    public float calculatePriority(AIContext context) {
        Player aiPlayer = context.getAiPlayer();
        float healthRatio = (float) aiPlayer.getLives() / (float) aiPlayer.getMaxHp();

        float adjustedCriticalThreshold = getAdjustedThreshold(context, CRITICAL_HEALTH_THRESHOLD);
        float adjustedLowThreshold = getAdjustedThreshold(context, LOW_HEALTH_THRESHOLD);

        // Health management gets higher priority when wounded
        if (healthRatio < adjustedCriticalThreshold) {
            return CRITICAL_HEALTH_PRIORITY;
        } else if (healthRatio < adjustedLowThreshold) {
            // Check if we're taking damage rapidly
            long currentTime = System.currentTimeMillis();
            float damageRate = (lastRecordedHealth - healthRatio) /
                    (Math.max(1f, (currentTime - lastHealthChangeTime)) / 1000);

            if (damageRate > HIGH_DAMAGE_RATE_THRESHOLD) {
                return HIGH_DAMAGE_RATE_PRIORITY;
            }
            return LOW_DAMAGE_RATE_PRIORITY;
        }

        return 0.0f;
    }

    @Override
    public String getStrategyName() {
        return "HEALTH_MANAGEMENT";
    }

    /**
     * Adjust retreat thresholds based on threat level
     * If facing much stronger enemies, retreat earlier
     */
    private float getAdjustedThreshold(AIContext context, float baseThreshold) {
        Player threat = context.getMemory().getCurrentThreat();
        if (threat == null || !threat.isAlive()) {
            return baseThreshold;
        }

        int levelDiff = threat.getLevel() - context.getAiPlayer().getLevel();

        if (levelDiff >= 5) {
            // Much stronger enemy - retreat more aggressively
            return baseThreshold / RETREAT_MULTIPLIER;
        } else if (levelDiff >= 1) {
            return baseThreshold / STRONGER_ENEMY_RETREAT_MULTIPLIER;
        }

        return baseThreshold;
    }

    private void executeCriticalRetreat(AIContext context) {
        // Move away from all threats toward map center or safe corner
        moveToBestRefuge(context);
        context.setShooting(false);
    }

    private void executeRetreat(AIContext context) {
        // Retreat from known threat but maintain some offensive capability
        Player threat = context.getMemory().getCurrentThreat();

        if (threat != null && threat.isAlive() && threat.getTank() != null) {
            moveAwayFrom(context,
                    threat.getTank().getPhysicsState().getX(),
                    threat.getTank().getPhysicsState().getY()
            );
        } else {
            moveToBestRefuge(context);
        }

        context.setShooting(false);
    }

    private void moveToBestRefuge(AIContext context) {
        // Calculate safest corner (farthest from all threats with level consideration)
        float bestX = 0;
        float bestY = 0;
        float bestSafety = Float.NEGATIVE_INFINITY;

        float[][] corners = {
                {ARENA_LOWER_BOUND_X, ARENA_LOWER_BOUND_Y},
                {ARENA_LOWER_BOUND_X, ARENA_UPPER_BOUND_Y},
                {ARENA_UPPER_BOUND_X, ARENA_LOWER_BOUND_Y},
                {ARENA_UPPER_BOUND_X, ARENA_UPPER_BOUND_Y},
                {(float) (ARENA_LOWER_BOUND_X + ARENA_UPPER_BOUND_X) / 2,
                        (float) (ARENA_LOWER_BOUND_Y + ARENA_UPPER_BOUND_Y) / 2} // Center
        };

        for (float[] corner : corners) {
            float safety = calculateSafety(context, corner[0], corner[1]);
            if (safety > bestSafety) {
                bestSafety = safety;
                bestX = corner[0];
                bestY = corner[1];
            }
        }

        moveTowards(context, bestX, bestY);
    }

    /**
     * Calculate safety score for a position
     */
    private float calculateSafety(AIContext context, float x, float y) {
        float safety = 1.0f;

        // Penalize proximity to threats, weighted by level difference
        for (Player player : context.getAllPlayers()) {
            // Process only living, valid enemy players
            if (player.isAlive() &&
                    player.getTank() != null &&
                    player.getId() != context.getAiPlayer().getId()) {

                float distToThreat = distance(x, y,
                        player.getTank().getPhysicsState().getX(),
                        player.getTank().getPhysicsState().getY()
                );

                int levelDiff = player.getLevel() - context.getAiPlayer().getLevel();
                float levelMultiplier = 1.0f + (Math.max(0, levelDiff) * THREAT_PER_LEVEL);

                float threatPenalty = (100 * levelMultiplier) / (distToThreat + 10);
                safety -= threatPenalty;
            }
        }

        return safety;
    }

    private void moveAwayFrom(AIContext context, float threatX, float threatY) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        float angle = (float) Math.atan2(aiY - threatY, aiX - threatX);
        int direction = MovementUtils.angleToDirectionMask(angle);
        context.setMovement(direction);
    }

    private void moveTowards(AIContext context, float targetX, float targetY) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        float angle = (float) Math.atan2(targetY - aiY, targetX - aiX);
        int direction = MovementUtils.angleToDirectionMask(angle);
        context.setMovement(direction);
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return MovementUtils.distance(x1, y1, x2, y2);
    }
}
