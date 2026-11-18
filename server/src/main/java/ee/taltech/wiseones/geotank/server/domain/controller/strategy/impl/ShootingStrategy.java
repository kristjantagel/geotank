package ee.taltech.wiseones.geotank.server.domain.controller.strategy.impl;

import ee.taltech.wiseones.geotank.server.domain.controller.strategy.AIStrategy;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIContext;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIMemory;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.prediction.BehaviorPredictor;
import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;

import static ee.taltech.wiseones.geotank.server.domain.controller.strategy.util.MovementUtils.distance;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.AI_PLAYER_DETECTION_RANGE;

public class ShootingStrategy implements AIStrategy {
    private static final float WEIGHT_PROXIMITY = 0.3f;      // 30% - prefer closer targets
    public static final float WEAK_TARGET_THRESHOLD = 0.4f;
    private static final float WEIGHT_LEVEL_DIFF = WEAK_TARGET_THRESHOLD;     // 40% - prefer weaker targets
    private static final float WEIGHT_HEALTH = 0.3f;         // 30% - prefer low-HP targets
    public static final float GOOD_SCORE_TRESHOLD = 0.5f;
    public static final float SCORE_PER_LEVEL = 0.2f;
    public static final float AI_HEALTH_THRESHOLD = 0.7f;
    public static final float NEUTRAL_SCORE = 0.8f;
    public static final float DEFAULT_PRIORITY = 0.92f;
    public static final float REASONABLE_PRIORITY = 0.85f;

    @Override
    public void execute(AIContext context) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        int aiLevel = context.getAiPlayer().getLevel();

        // Find best shooting target using weighted priorities
        ShootingTarget bestTarget = findBestShootingTarget(context, aiX, aiY, aiLevel);

        if (bestTarget != null) {
            // Aim and shoot
            float aimAngle = bestTarget.getAimAngle();
            context.setFacing(aimAngle);
            context.setShooting(true);
        } else {
            // No valid target
            context.setShooting(false);
        }
    }

    private ShootingTarget findBestShootingTarget(AIContext context, float aiX, float aiY, int aiLevel) {
        ShootingTarget bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        // Try to find the best player target
        for (Player player : context.getAllPlayers()) {
            if (!isValidPlayerTarget(player, context)) {
                continue;
            }

            Tank playerTank = player.getTank();
            float targetX = playerTank.getPhysicsState().getX();
            float targetY = playerTank.getPhysicsState().getY();

            // Check if in range
            if (!context.getBulletRangeService().isTargetInRange(
                    context.getAiPlayer(), targetX, targetY, aiX, aiY)) {
                continue;
            }

            // Calculate weighted priority score
            float score = calculatePlayerTargetScore(context, player, aiX, aiY, aiLevel);

            if (score > bestScore) {
                bestScore = score;
                bestTarget = createPlayerShootingTarget(context, player);
            }
        }

        // If good player target found, return it
        if (bestTarget != null && bestScore > GOOD_SCORE_TRESHOLD) {
            return bestTarget;
        }

        // Fall back to destructibles
        Destructible destructible = findBestDestructible(context, aiX, aiY);
        if (destructible != null && context.getBulletRangeService().isTargetInRange(
                context.getAiPlayer(), destructible.getX(), destructible.getY(), aiX, aiY)) {
            // Aim directly at destructible current position
            float aimAngle = (float) Math.atan2(destructible.getY() - aiY, destructible.getX() - aiX);
            return new ShootingTarget(aimAngle);
        }
        return bestTarget;
    }

    /**
     * Calculate weighted priority score for a player target
     */
    private float calculatePlayerTargetScore(AIContext context, Player target, float aiX, float aiY, int aiLevel) {
        float targetX = target.getTank().getPhysicsState().getX();
        float targetY = target.getTank().getPhysicsState().getY();

        int levelDiff = target.getLevel() - aiLevel;
        float distToTarget = distance(aiX, aiY, targetX, targetY);
        float targetHealthRatio = (float) target.getLives() / (float) target.getMaxHp();
        float aiHealthRatio = (float) context.getAiPlayer().getLives() / (float) context.getAiPlayer().getMaxHp();

        // Proximity
        float proximityScore = calculateProximityScore(distToTarget);

        // Level difference
        float levelScore = calculateLevelScore(levelDiff);

        // Health
        float healthScore = calculateHealthScore(aiHealthRatio, targetHealthRatio);

        // Combined weighted score
        return (proximityScore * WEIGHT_PROXIMITY) + (levelScore * WEIGHT_LEVEL_DIFF) + (healthScore * WEIGHT_HEALTH);
    }

    private float calculateProximityScore(float distance) {
        // Normalize by a reasonable combat distance (300 units)
        float normalizedDist = Math.min(distance / AI_PLAYER_DETECTION_RANGE, 1.0f);
        return 1.0f - normalizedDist;
    }

    private float calculateLevelScore(int levelDiff) {
        return -Math.clamp(levelDiff * SCORE_PER_LEVEL, -1.0f, 1.0f);
    }

    private float calculateHealthScore(float aiHealthRatio, float targetHealthRatio) {
        // Strong advantage
        if (aiHealthRatio > AI_HEALTH_THRESHOLD && targetHealthRatio < GOOD_SCORE_TRESHOLD) {
            return 1.0f;
        }

        // Moderate advantage
        if ((aiHealthRatio > AI_HEALTH_THRESHOLD) || (targetHealthRatio < WEAK_TARGET_THRESHOLD)) {
            return NEUTRAL_SCORE;
        }

        // Balanced situation
        if (Math.abs(aiHealthRatio - targetHealthRatio) < SCORE_PER_LEVEL) {
            return GOOD_SCORE_TRESHOLD;
        }

        // Bad situation
        if (aiHealthRatio < WEAK_TARGET_THRESHOLD && targetHealthRatio > WEAK_TARGET_THRESHOLD) {
            return SCORE_PER_LEVEL;
        }

        return GOOD_SCORE_TRESHOLD;
    }

    /**
     * Check if a player is a valid shooting target
     */
    private boolean isValidPlayerTarget(Player player, AIContext context) {
        return player.isAlive() && player.getTank() != null
                && player.getId() != context.getAiPlayer().getId();
    }

    /**
     * Find the best destructible to shoot
     */
    private Destructible findBestDestructible(AIContext context, float aiX, float aiY) {
        Destructible closest = null;
        float closestDistance = Float.MAX_VALUE;

        for (Destructible destructible : context.getDestructibles()) {
            if (destructible.getDestroyedTime() != 0) {
                continue;
            }

            float distance = distance(aiX, aiY, destructible.getX(), destructible.getY());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = destructible;
            }
        }

        return closest;
    }

    /**
     * Create a shooting target with predicted aim point
     */
    private ShootingTarget createPlayerShootingTarget(AIContext context, Player target) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();

        AIMemory.EnemyProfile profile = context.getMemory().getOrCreateEnemyProfile(target);
        BehaviorPredictor predictor = context.getBehaviorPredictor();
        BehaviorPredictor.PredictedPosition predicted = predictor.predictEnemyPosition(
                profile,
                context.getAiTank(),
                aiX,
                aiY
        );

        float aimAngle = (float) Math.atan2(predicted.y() - aiY, predicted.x() - aiX);
        return new ShootingTarget(aimAngle);
    }

    @Override
    public float calculatePriority(AIContext context) {
        float healthRatio = (float) context.getAiPlayer().getLives() / (float) context.getAiPlayer().getMaxHp();

        if (healthRatio < SCORE_PER_LEVEL) {
            return AI_HEALTH_THRESHOLD;
        } else if (healthRatio < GOOD_SCORE_TRESHOLD) {
            return REASONABLE_PRIORITY;
        }
        return DEFAULT_PRIORITY;
    }

    @Override
    public String getStrategyName() {
        return "SHOOTING";
    }

    /**
     * Inner class to encapsulate shooting target information
     */
    private static class ShootingTarget {
        final float aimAngle;

        ShootingTarget(float aimAngle) {
            this.aimAngle = aimAngle;
        }

        float getAimAngle() {
            return aimAngle;
        }
    }
}
