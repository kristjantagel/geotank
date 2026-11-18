package ee.taltech.wiseones.geotank.server.domain.controller.strategy;

import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIContext;

/**
 * Strategy interface for AI behavior.
 */
public interface AIStrategy {
    /**
     * Executes the decision-making for a single game tick.
     *
     * @param context context containing game state and tank reference
     */
    void execute(AIContext context);

    /**
     * Determines the priority of this strategy relative to other strategies.
     *
     * @param context context for context-aware priority calculation
     * @return priority value
     */
    float calculatePriority(AIContext context);

    /**
     * Strategy name.
     *
     * @return strategy name (e.g., "COMBAT", "EVASION")
     */
    String getStrategyName();
}
