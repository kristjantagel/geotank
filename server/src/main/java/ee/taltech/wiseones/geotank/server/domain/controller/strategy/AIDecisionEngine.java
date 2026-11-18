package ee.taltech.wiseones.geotank.server.domain.controller.strategy;

import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIContext;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.util.MovementUtils;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import lombok.Getter;

import java.util.*;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.AI_PLAYER_DETECTION_RANGE;

/**
 * AI Decision Engine using Strategy pattern with priority-based selection.
 * Orchestrates multiple AI strategies and chooses the best one each frame.
 */
public class AIDecisionEngine {
    public static final float AI_HP_THRESHHOLD = 0.8f;
    public static final float TARGET_HP_THRESHHOLD = 0.5f;
    public static final float HEALTH_ADVANTAGE_BONUS = 0.3f;
    public static final float MIN_PRIORITY = 0.1f;
    private final List<AIStrategy> strategies;

    @Getter
    private AIStrategy activeStrategy;

    public AIDecisionEngine(List<AIStrategy> strategies) {
        this.strategies = new ArrayList<>(strategies);
        this.activeStrategy = strategies.isEmpty() ? null : strategies.getFirst();
    }

    /**
     * Update AI decision and execute strategies
     */
    public void update(AIContext context) {
        updateMemory(context);

        // Execute all strategies with priority >= 0.1f, sorted by priority (lowest first)
        // Higher priority strategies execute last and can override facing/movement from lower priority ones
        strategies.stream()
                .filter(strategy -> strategy.calculatePriority(context) >= MIN_PRIORITY)
                .sorted((s1, s2) -> Float.compare(s1.calculatePriority(context), s2.calculatePriority(context)))
                .forEach(strategy -> {
                    strategy.execute(context);
                    activeStrategy = strategy;
                });
    }

    /**
     * Update AI memory with latest game state
     */
    private void updateMemory(AIContext context) {
        Player currentThreat = context.getMemory().getCurrentThreat();

        for (Player player : context.getAllPlayers()) {
            if (player.getId() == context.getAiPlayer().getId()) {
                continue;
            }

            if (!player.isAlive()) {
                context.getMemory().removeEnemyProfile(player.getId());
            } else if (player.getTank() != null) {
                var physics = player.getTank().getPhysicsState();
                context.getMemory().updateEnemyProfile(
                        player,
                        physics.getX(),
                        physics.getY(),
                        physics.getSpeed().x(),
                        physics.getSpeed().y()
                );
            }
        }

        // If no current threat, find closest alive player
        if (currentThreat == null || !currentThreat.isAlive()) {
            Player closest = findBestTarget(context);
            context.getMemory().setCurrentThreat(closest);
        } else {
            // Decay aggressiveness of other enemies over time
            for (var profile : context.getMemory().getEnemyProfiles().values()) {
                profile.decayAggressiveness();
            }
        }

        // hp
        float currentHealthRatio = (float) context.getAiPlayer().getLives() / (float) context.getAiPlayer().getMaxHp();
        context.getMemory().setLastHealthPercentage(currentHealthRatio);
    }

    /**
     * Find best target, takes into account level, distance and hp
     */
    private Player findBestTarget(AIContext context) {
        Player bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        float aiX = context.getAiX();
        float aiY = context.getAiY();
        int aiLevel = context.getAiPlayer().getLevel();
        float aiHealth = (float) context.getAiPlayer().getLives() / (float) context.getAiPlayer().getMaxHp();

        for (Player player : context.getAllPlayers()) {
            if (player.getId() == context.getAiPlayer().getId() || !player.isAlive() || player.getTank() == null) {
                continue;
            }

            // Don't engage enemies 3+ levels higher
            int levelDiff = player.getLevel() - aiLevel;

            if (levelDiff < 3) {
                float distance = distance(aiX, aiY,
                        player.getTank().getPhysicsState().getX(),
                        player.getTank().getPhysicsState().getY());

                // Prefer wounded targets when healthy
                float targetHealth = (float) player.getLives() / (float) player.getMaxHp();
                float healthAdvantageBonus = 0;
                if (aiHealth > AI_HP_THRESHHOLD && targetHealth < TARGET_HP_THRESHHOLD) {
                    healthAdvantageBonus = HEALTH_ADVANTAGE_BONUS;
                }

                // Calculate target score
                float score = calculateTargetScore(distance, levelDiff) + healthAdvantageBonus;

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = player;
                }
            }
        }

        return bestTarget;
    }

    /**
     * Calculate a score for target prioritization.
     */
    private float calculateTargetScore(float distance, int levelDiff) {
        // Add small constant to avoid division issues
        float distanceScore = AI_PLAYER_DETECTION_RANGE / (distance + 10);

        // prefer lower level enemies
        float levelMultiplier = Math.max(0, 1.0f - ((float) levelDiff / 5));

        return distanceScore * levelMultiplier;
    }

    public String getActiveStrategyName() {
        return activeStrategy != null ? activeStrategy.getStrategyName() : "NONE";
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return MovementUtils.distance(x1, y1, x2, y2);
    }
}
