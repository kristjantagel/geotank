package ee.taltech.wiseones.geotank.server.domain.controller.strategy.context;

import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.pathfinding.PathfindingService;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.prediction.BehaviorPredictor;
import ee.taltech.wiseones.geotank.server.domain.controller.strategy.prediction.BulletRangeService;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * Contains information needed for decision-making.
 */
@Getter
@Setter
public class AIContext {
    private Tank aiTank;
    private Player aiPlayer;

    private Collection<Player> allPlayers;
    private Collection<Destructible> destructibles;
    private Collection<Projectile> projectiles;

    private float timeDeltaMs;
    private long currentTimeMs;

    // Shared services for decision-making
    private PathfindingService pathfindingService;
    private BehaviorPredictor behaviorPredictor;
    private BulletRangeService bulletRangeService;

    private AIMemory memory;

    public AIContext(Tank aiTank, Player aiPlayer,
                     Collection<Player> allPlayers,
                     Collection<Destructible> destructibles,
                     Collection<Projectile> projectiles,
                     float timeDeltaMs,
                     long currentTimeMs,
                     PathfindingService pathfindingService,
                     BehaviorPredictor behaviorPredictor,
                     BulletRangeService bulletRangeService,
                     AIMemory memory) {
        this.aiTank = aiTank;
        this.aiPlayer = aiPlayer;
        this.allPlayers = allPlayers;
        this.destructibles = destructibles;
        this.projectiles = projectiles;
        this.timeDeltaMs = timeDeltaMs;
        this.currentTimeMs = currentTimeMs;
        this.pathfindingService = pathfindingService;
        this.behaviorPredictor = behaviorPredictor;
        this.bulletRangeService = bulletRangeService;
        this.memory = memory;
    }

    public float getAiX() {
        return aiTank.getPhysicsState().getX();
    }

    public float getAiY() {
        return aiTank.getPhysicsState().getY();
    }

    public void setMovement(int directionMask) {
        aiTank.setDirectionMask(directionMask);
    }

    public void setFacing(float angleRad) {
        aiTank.setFacingAngle(angleRad);
    }

    public void setShooting(boolean shouldShoot) {
        aiTank.setWantsToShoot(shouldShoot);
    }
}
