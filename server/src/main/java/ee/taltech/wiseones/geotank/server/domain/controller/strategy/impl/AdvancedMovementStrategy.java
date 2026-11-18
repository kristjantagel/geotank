package ee.taltech.wiseones.geotank.server.domain.controller.strategy.impl;

import ee.taltech.wiseones.geotank.server.domain.controller.strategy.AIStrategy;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIContext;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.pathfinding.PathfindingService;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.util.MovementUtils;
import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;

import static ee.taltech.wiseones.geotank.server.domain.controller.strategy.util.MovementUtils.distance;

/**
 * Uses A* pathfinding for better navigation.
 */
public class AdvancedMovementStrategy implements AIStrategy {
    private static final float BASE_COMBAT_DISTANCE = 200f;
    public static final float WAYPOINT_DISTANCE_THRESHOLD = 50f;
    private static final float COMBAT_DISTANCE_TOLERANCE = WAYPOINT_DISTANCE_THRESHOLD;
    private static final float STRATEGIC_SEARCH_RANGE = 400f;
    private static final float PATHFINDING_TRIGGER_RANGE = 300f;  // Use A* only when this far from target
    private static final float PATHFINDING_CLOSE_RANGE = 100f;    // Stop using A* when this close

    public static final float SCORE_PER_LEVEL = 0.4f;
    public static final float AI_HP_THRESHOLD = 0.7f;
    public static final float TARGET_HP_THRESHOLD = 0.5f;
    public static final float HP_SCORE_BONUS = 0.6f;
    public static final int STRAFE_FREQUENCY = 2000;
    public static final float LEVEL_MULTIPLIER = 30f;
    public static final float ADJUSTMENT_MULTIPLIER = 0.3f;
    public static final float MOVEMENT_PRIORITY = 0.45f;
    public static final float DEFAULT_LEVEL_SCORE = 0.2f;

    private long lastCombatChangeTime = 0;
    private int lastCombatDirection = 1;
    private PathfindingService.Path currentPath = null;
    private int nextWaypointIndex = 0;

    @Override
    public void execute(AIContext context) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        int aiLevel = context.getAiPlayer().getLevel();

        // Find the best movement target
        Player targetPlayer = findBestMovementTarget(context, aiX, aiY, aiLevel);
        Destructible closestDestructible = findClosestDestructible(context, aiX, aiY);

        handleMovement(context, targetPlayer, closestDestructible, aiX, aiY, aiLevel);
    }

    private void handleMovement(AIContext context, Player targetPlayer, Destructible destructible,
                                float aiX, float aiY, int aiLevel) {
        if (targetPlayer != null && targetPlayer.isAlive() && targetPlayer.getTank() != null) {
            float playerX = targetPlayer.getTank().getPhysicsState().getX();
            float playerY = targetPlayer.getTank().getPhysicsState().getY();
            float distToPlayer = distance(aiX, aiY, playerX, playerY);

            int levelDiff = targetPlayer.getLevel() - aiLevel;
            float adaptiveDistance = calculateAdaptiveCombatDistance(levelDiff);

            // Direct combat positioning when close
            if (distToPlayer < PATHFINDING_CLOSE_RANGE) {
                handleDirectCombat(context, playerX, playerY, adaptiveDistance, targetPlayer);
                currentPath = null;  // Clear path when in direct combat
                nextWaypointIndex = 0;
                return;
            }

            // Use A* pathfinding for long-distance approach
            if (distToPlayer > PATHFINDING_TRIGGER_RANGE) {
                handlePathfindingMovement(context, playerX, playerY, aiX, aiY);
                return;
            }

            if (distToPlayer > adaptiveDistance + COMBAT_DISTANCE_TOLERANCE) {
                moveTowards(context, playerX, playerY);
            } else if (distToPlayer < adaptiveDistance - COMBAT_DISTANCE_TOLERANCE) {
                moveAway(context, playerX, playerY);
            } else {
                strafeTarget(context, playerX, playerY, targetPlayer);
            }
            return;
        }

        // Move toward destructible if no good player target
        if (destructible != null && destructible.getDestroyedTime() == 0) {
            moveTowards(context, destructible.getX(), destructible.getY());
        }
    }

    /**
     * Handle direct combat positioning (existing logic)
     */
    private void handleDirectCombat(AIContext context, float playerX, float playerY,
                                    float adaptiveDistance, Player targetPlayer) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        float distToPlayer = distance(aiX, aiY, playerX, playerY);

        if (distToPlayer > adaptiveDistance + COMBAT_DISTANCE_TOLERANCE) {
            moveTowards(context, playerX, playerY);
        } else if (distToPlayer < adaptiveDistance - COMBAT_DISTANCE_TOLERANCE) {
            moveAway(context, playerX, playerY);
        } else {
            strafeTarget(context, playerX, playerY, targetPlayer);
        }
    }

    /**
     * Handle pathfinding-based movement for distant targets
     */
    private void handlePathfindingMovement(AIContext context, float targetX, float targetY,
                                           float aiX, float aiY) {
        // Recalculate path if needed
        if (currentPath == null || currentPath.isStale()) {
            currentPath = context.getPathfindingService().findPath(aiX, aiY, targetX, targetY, null);
            nextWaypointIndex = 0;
        }

        // Follow the path if available
        if (currentPath != null && !currentPath.isEmpty()) {
            if (nextWaypointIndex >= currentPath.waypoints.size()) {
                nextWaypointIndex = 0;  // Reset if reached end
            }

            PathfindingService.Waypoint nextWaypoint = currentPath.waypoints.get(nextWaypointIndex);
            float distToWaypoint = distance(aiX, aiY, nextWaypoint.x, nextWaypoint.y);

            // Move to waypoint, advance when close
            if (distToWaypoint < WAYPOINT_DISTANCE_THRESHOLD) {
                nextWaypointIndex++;
            }

            moveTowards(context, nextWaypoint.x, nextWaypoint.y);
        } else {
            // direct movement if pathfinding fails
            moveTowards(context, targetX, targetY);
        }
    }

    /**
     * Find the best player target for movement positioning
     */
    private Player findBestMovementTarget(AIContext context, float aiX, float aiY, int aiLevel) {
        Player bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (Player player : context.getAllPlayers()) {
            if (player.isAlive() && player.getTank() != null && player.getId() != context.getAiPlayer().getId() &&
                    distance(aiX, aiY, player.getTank().getPhysicsState().getX(),
                            player.getTank().getPhysicsState().getY()) <= STRATEGIC_SEARCH_RANGE) {

                int levelDiff = player.getLevel() - aiLevel;
                float healthRatio = (float) player.getLives() / (float) player.getMaxHp();
                float aiHealthRatio = (float) context.getAiPlayer().getLives() / context.getAiPlayer().getMaxHp();
                float distance = distance(aiX, aiY, player.getTank().getPhysicsState().getX(),
                        player.getTank().getPhysicsState().getY());
                float score = calculateMovementTargetScore(levelDiff, healthRatio, aiHealthRatio, distance);

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = player;
                }
            }
        }
        return bestTarget;
    }

    /**
     * Calculate movement target score
     */
    private float calculateMovementTargetScore(int levelDiff, float targetHealth, float aiHealth, float distance) {
        float levelScore = levelDiff < 0 ? 1.0f - (Math.abs(levelDiff) * SCORE_PER_LEVEL) : DEFAULT_LEVEL_SCORE;
        float healthScore = targetHealth < TARGET_HP_THRESHOLD && aiHealth > AI_HP_THRESHOLD ? HP_SCORE_BONUS : 0;
        float distanceScore = 1.0f - Math.min(distance / STRATEGIC_SEARCH_RANGE, 1.0f);
        return levelScore + healthScore + distanceScore;
    }

    /**
     * Calculate adaptive combat distance based on level difference
     */
    private float calculateAdaptiveCombatDistance(int levelDiff) {
        if (levelDiff < 0) return BASE_COMBAT_DISTANCE;
        return BASE_COMBAT_DISTANCE - (levelDiff * LEVEL_MULTIPLIER * ADJUSTMENT_MULTIPLIER);
    }

    /**
     * Strafe target (existing combat logic)
     */
    private void strafeTarget(AIContext context, float targetX, float targetY, Player target) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        float angle = MovementUtils.angle(aiX, aiY, targetX, targetY);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCombatChangeTime > STRAFE_FREQUENCY + Math.random() * 1000) {
            lastCombatChangeTime = currentTime;
            lastCombatDirection *= -1;
        }

        float strafeAngle = angle + (float) (Math.PI / 2) * lastCombatDirection;

        // Incorporate target velocity for dodge prediction
        var profile = context.getMemory().getOrCreateEnemyProfile(target);
        float perpVelX = -profile.getVelocityY();
        float perpVelY = profile.getVelocityX();
        float perpVelMag = (float) Math.sqrt(perpVelX * perpVelX + perpVelY * perpVelY);

        if (perpVelMag > (float) 1 / 10) {
            float adjustment = (float) Math.atan2(perpVelY, perpVelX);
            strafeAngle += adjustment * ADJUSTMENT_MULTIPLIER;
        }

        int directionMask = MovementUtils.angleToDirectionMask(strafeAngle);
        context.setMovement(directionMask);
    }

    private void moveAway(AIContext context, float targetX, float targetY) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        float retreatAngle = (float) Math.atan2(aiY - targetY, aiX - targetX);
        float variation = ((float) Math.random() - TARGET_HP_THRESHOLD) * (float) (Math.PI / 6);
        retreatAngle += variation;

        int directionMask = MovementUtils.angleToDirectionMask(retreatAngle);
        context.setMovement(directionMask);
    }

    private void moveTowards(AIContext context, float targetX, float targetY) {
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        int directionMask = MovementUtils.calculateDirectionMask(aiX, aiY, targetX, targetY);
        context.setMovement(directionMask);
    }

    private Destructible findClosestDestructible(AIContext context, float aiX, float aiY) {
        Destructible closest = null;
        float closestDistance = Float.MAX_VALUE;

        for (Destructible destructible : context.getDestructibles()) {
            if (destructible.getDestroyedTime() != 0) continue;
            float distance = distance(aiX, aiY, destructible.getX(), destructible.getY());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = destructible;
            }
        }
        return closest;
    }

    @Override
    public float calculatePriority(AIContext context) {
        return MOVEMENT_PRIORITY;
    }

    @Override
    public String getStrategyName() {
        return "ADVANCED_MOVEMENT";
    }
}
