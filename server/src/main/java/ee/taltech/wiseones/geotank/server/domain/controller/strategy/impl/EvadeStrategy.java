package ee.taltech.wiseones.geotank.server.domain.controller.strategy.impl;

import ee.taltech.wiseones.geotank.server.domain.controller.strategy.AIStrategy;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIContext;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.util.MovementUtils;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;

import static ee.taltech.wiseones.geotank.server.domain.controller.strategy.util.MovementUtils.*;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Evasion Strategy, movement only threat avoidance.
 */
public class EvadeStrategy implements AIStrategy {
    private static final float THREAT_DETECTION_RANGE = 350f;
    private static final float TANK_COLLISION_RADIUS = 25f;
    private static final int UP = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 4;
    private static final int RIGHT = 8;
    public static final float DEFAULT_PROJECTILE_THREAT_SCORE = 0.3f;
    public static final float HEALTH_FACTOR_HIGH = 1.5f;
    public static final float HEALTH_RATIO_MID = 0.6f;
    public static final float HEALTH_FACTOR_MID = 1.2f;
    public static final float PROJECTILE_THREAT_SCORE_MULTIPLIER = 0.15f;
    public static final float BASE_PROJECTILE_THREAT_SCORE = 0.85f;
    public static final float MIN_PRIORITY = 0.98f;
    public static final float COLLISION_LIKELIHOOD_MULTIPLIER = 0.4f;
    public static final float BORDER_BUFFER_MULTIPLIER = 0.7f;

    @Override
    public void execute(AIContext context) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();

        // Evade incoming projectiles
        Projectile threateningProjectile = findThreateningProjectile(context, aiX, aiY);
        if (threateningProjectile != null) {
            int bestEscapeDir = calculateBestEscapeDirection(aiX, aiY, threateningProjectile);
            if (bestEscapeDir > 0) {
                context.setMovement(bestEscapeDir);
                return;
            }
        }

        //  Move away from significantly stronger players
        Player highLevelThreat = findSignificantlyStrongerPlayer(context, aiX, aiY);
        if (highLevelThreat != null && highLevelThreat.getTank() != null) {
            float threatX = highLevelThreat.getTank().getPhysicsState().getX();
            float threatY = highLevelThreat.getTank().getPhysicsState().getY();

            float awayAngle = (float) Math.atan2(aiY - threatY, aiX - threatX);
            int directionMask = angleToDirectionMask(awayAngle);
            context.setMovement(directionMask);
        }
    }

    @Override
    public float calculatePriority(AIContext context) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();

        // Calculate threat score from multiple sources
        float projectileThreatScore = calculateCombinedThreatScore(context, aiX, aiY);

        if (projectileThreatScore < (float) 1 / 10) {
            // Check for high-level players
            Player strongThreat = findSignificantlyStrongerPlayer(context, aiX, aiY);
            if (strongThreat == null) {
                return 0.0f;  // No threats
            }
            projectileThreatScore = DEFAULT_PROJECTILE_THREAT_SCORE;
        }

        // Evasion gets high priority when threatened
        float healthRatio = (float) context.getAiPlayer().getLives() / (float) context.getAiPlayer().getMaxHp();

        // Critical health increases evasion priority
        float healthFactor;
        if (healthRatio < DEFAULT_PROJECTILE_THREAT_SCORE) {
            healthFactor = HEALTH_FACTOR_HIGH;
        } else if (healthRatio < HEALTH_RATIO_MID) {
            healthFactor = HEALTH_FACTOR_MID;
        } else {
            healthFactor = 1.0f - (healthRatio * 1 / 10);
        }

        return Math.min(MIN_PRIORITY, (BASE_PROJECTILE_THREAT_SCORE + projectileThreatScore
                * PROJECTILE_THREAT_SCORE_MULTIPLIER) * healthFactor);
    }

    @Override
    public String getStrategyName() {
        return "EVASION";
    }

    private float calculateSingleProjectileThreat(Projectile projectile, float tankX, float tankY) {
        float projX = projectile.getPhysicsState().getX();
        float projY = projectile.getPhysicsState().getY();
        float distance = distance(tankX, tankY, projX, projY);

        if (distance > THREAT_DETECTION_RANGE) {
            return 0.0f;
        }

        float projSpeedX = projectile.getPhysicsState().getSpeed().x();
        float projSpeedY = projectile.getPhysicsState().getSpeed().y();

        float directionTankX = tankX - projX;
        float directionTankY = tankY - projY;
        float dotProduct = projSpeedX * directionTankX + projSpeedY * directionTankY;

        // Moving away from or parallel to the tank
        if (dotProduct <= 0) {
            return 0.0f;
        }

        float proximityScore = 1.0f - (distance / THREAT_DETECTION_RANGE);
        float collisionLikelihood = calculateCollisionLikelihood(tankX, tankY, projX, projY, projSpeedX, projSpeedY);

        return (proximityScore * HEALTH_RATIO_MID) + (collisionLikelihood * COLLISION_LIKELIHOOD_MULTIPLIER);
    }

    /**
     * Calculate combined threat from all nearby projectiles
     */
    private float calculateCombinedThreatScore(AIContext context, float tankX, float tankY) {
        float maxThreat = 0;
        int threatCount = 0;

        for (Projectile projectile : context.getProjectiles()) {
            float threatScore = calculateSingleProjectileThreat(projectile, tankX, tankY);

            if (threatScore > 0) {
                maxThreat = Math.max(maxThreat, threatScore);
                threatCount++;
            }
        }

        if (threatCount > 1) {
            maxThreat = Math.min(1.0f, maxThreat + (float) 1 / 10 * (threatCount - 1));
        }

        return maxThreat;
    }

    private Player findSignificantlyStrongerPlayer(AIContext context, float aiX, float aiY) {
        int aiLevel = context.getAiPlayer().getLevel();
        Player strongestThreat = null;
        float closestDistance = Float.MAX_VALUE;

        for (Player player : context.getAllPlayers()) {
            // Combined validation guard check
            if (player.isAlive() &&
                    player.getTank() != null &&
                    player.getId() != context.getAiPlayer().getId() &&
                    (player.getLevel() - aiLevel) >= 3) {

                float distance = distance(aiX, aiY,
                        player.getTank().getPhysicsState().getX(),
                        player.getTank().getPhysicsState().getY());

                // Check if the valid threat is within range and is the closest one found so far
                if (distance < AI_PLAYER_DETECTION_RANGE && distance < closestDistance) {
                    closestDistance = distance;
                    strongestThreat = player;
                }
            }
        }
        return strongestThreat;
    }

    /**
     * Calculate best escape direction from a projectile
     */
    private int calculateBestEscapeDirection(float tankX, float tankY, Projectile projectile) {
        float projSpeedX = projectile.getPhysicsState().getSpeed().x();
        float projSpeedY = projectile.getPhysicsState().getSpeed().y();

        // Use boundary buffer as escape look-ahead to ensure good wall clearance
        float escapeDistance = AI_BOUNDARY_BUFFER * BORDER_BUFFER_MULTIPLIER;

        int[] escapeDirections = generateEscapeDirections(projSpeedX, projSpeedY);

        for (int direction : escapeDirections) {
            if (isDirectionSafe(tankX, tankY, direction, escapeDistance)) {
                return direction;
            }
        }

        int[] cardinalDirections = {UP, DOWN, LEFT, RIGHT};
        for (int direction : cardinalDirections) {
            if (isDirectionSafe(tankX, tankY, direction, escapeDistance)) {
                return direction;
            }
        }

        return 0;
    }

    /**
     * Generate escape directions prioritized by safety
     */
    private int[] generateEscapeDirections(float projSpeedX, float projSpeedY) {
        int perp1 = angleToDirectionMask((float) Math.atan2(projSpeedX, -projSpeedY));
        int perp2 = angleToDirectionMask((float) Math.atan2(-projSpeedX, projSpeedY));

        return new int[]{
                perp1,
                perp2,
                UP | LEFT,
                UP | RIGHT,
                DOWN | LEFT,
                DOWN | RIGHT,
                UP,
                DOWN,
                LEFT,
                RIGHT
        };
    }

    /**
     * Find the most threatening projectile
     */
    private Projectile findThreateningProjectile(AIContext context, float tankX, float tankY) {
        Projectile mostThreatening = null;
        float highestThreatScore = 0;

        for (Projectile projectile : context.getProjectiles()) {
            float threatScore = calculateSingleProjectileThreat(projectile, tankX, tankY);

            if (threatScore > highestThreatScore) {
                highestThreatScore = threatScore;
                mostThreatening = projectile;
            }
        }
        return mostThreatening;
    }

    /**
     * Calculate collision likelihood based on trajectory
     */
    private float calculateCollisionLikelihood(float tankX, float tankY, float projX, float projY,
                                               float projSpeedX, float projSpeedY) {
        float relX = tankX - projX;
        float relY = tankY - projY;

        float speedMag = (float) Math.sqrt(projSpeedX * projSpeedX + projSpeedY * projSpeedY);
        if (speedMag < (float) 1 / 10) return 0.0f;

        float normSpeedX = projSpeedX / speedMag;
        float normSpeedY = projSpeedY / speedMag;

        float alongDirection = relX * normSpeedX + relY * normSpeedY;
        float perpDistance = (float) Math.sqrt(relX * relX + relY * relY - alongDirection * alongDirection);

        return Math.max(0.0f, 1.0f - (perpDistance / TANK_COLLISION_RADIUS));
    }

    /**
     * Check if moving in a direction is safe (doesn't hit boundaries)
     */
    private boolean isDirectionSafe(float tankX, float tankY, int direction, float distance) {
        float newX = tankX;
        float newY = tankY;

        if ((direction & UP) != 0) newY += distance;
        if ((direction & DOWN) != 0) newY -= distance;
        if ((direction & LEFT) != 0) newX -= distance;
        if ((direction & RIGHT) != 0) newX += distance;

        // Check if position would be within boundaries with buffer
        return newX >= ARENA_LOWER_BOUND_X + AI_BOUNDARY_BUFFER &&
                newX <= ARENA_UPPER_BOUND_X - AI_BOUNDARY_BUFFER &&
                newY >= ARENA_LOWER_BOUND_Y + AI_BOUNDARY_BUFFER &&
                newY <= ARENA_UPPER_BOUND_Y - AI_BOUNDARY_BUFFER;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return MovementUtils.distance(x1, y1, x2, y2);
    }
}
