package ee.taltech.wiseones.geotank.server.domain.controller;

import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.*;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIContext;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIMemory;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.impl.*;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.pathfinding.PathfindingService;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.prediction.BehaviorPredictor;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.prediction.BulletRangeService;
import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Strategy-based AI Logic using the Strategy pattern with a decision engine.
 */
public class StrategyBasedAILogic implements AILogic {
    private final AIDecisionEngine decisionEngine;
    private final PathfindingService pathfindingService;
    private final BehaviorPredictor behaviorPredictor;
    private final BulletRangeService bulletRangeService;
    private final AIMemory aiMemory;
    @Getter
    private long lastStrategyChangeTime = 0;
    @Getter
    private String lastActiveStrategy = "NONE";

    public StrategyBasedAILogic() {
        // Initialize shared services
        this.pathfindingService = new PathfindingService(
                50f, // Grid size
                ARENA_LOWER_BOUND_X, ARENA_UPPER_BOUND_X,
                ARENA_LOWER_BOUND_Y, ARENA_UPPER_BOUND_Y
        );
        this.behaviorPredictor = new BehaviorPredictor();
        this.bulletRangeService = new BulletRangeService();
        this.aiMemory = new AIMemory();

        // Initialize strategies with priority-based selection
        List<AIStrategy> strategies = initializeStrategies();

        // Create decision engine with all strategies
        this.decisionEngine = new AIDecisionEngine(strategies);
    }

    private List<AIStrategy> initializeStrategies() {
        List<AIStrategy> strategies = new ArrayList<>();

        strategies.add(new ShootingStrategy());
        strategies.add(new EvadeStrategy());
        strategies.add(new HealthManagementStrategy());
        strategies.add(new AdvancedMovementStrategy());

        return strategies;
    }

    @Override
    public void update(Tank tank, Collection<Player> otherPlayers, Collection<Destructible> destructibles,
                       Collection<Projectile> projectiles, float timeDeltaMs, GameWorld gameWorld) {

        Player currentPlayer = gameWorld.getPlayerManager().getPlayerByTank(tank);
        if (currentPlayer == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Create context with all necessary information
        AIContext context = new AIContext(
                tank,
                currentPlayer,
                otherPlayers,
                destructibles,
                projectiles,
                timeDeltaMs,
                currentTime,
                pathfindingService,
                behaviorPredictor,
                bulletRangeService,
                aiMemory
        );

        // Let decision engine select and execute the best strategy
        decisionEngine.update(context);

        String newStrategy = decisionEngine.getActiveStrategyName();
        if (!newStrategy.equals(lastActiveStrategy)) {
            lastStrategyChangeTime = currentTime;
            lastActiveStrategy = newStrategy;
        }
    }
}
