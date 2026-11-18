package ee.taltech.wiseones.geotank.server.domain.controller.strategy.prediction;

import ee.taltech.wiseones.geotank.server.domain.controller.strategy.context.AIMemory;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;

/**
 * Predicts enemy behavior and movement patterns.
 * Uses historical data to anticipate enemy positions and actions.
 */
public class BehaviorPredictor {

    public static final float DEFAULT_CONFIDENCE = 0.3f;
    public static final float CONFIDENCE_DECAY = 0.7f;

    /**
     * Predict where an enemy will be when the bullet reaches them.
     *
     * @param profile enemy profile
     * @param shooterTank tank of the shooter (to get actual bullet speed)
     * @param shooterX X position of the shooter
     * @param shooterY Y position of the shooter
     * @return predicted position where bullet will hit
     */
    public PredictedPosition predictEnemyPosition(AIMemory.EnemyProfile profile, Tank shooterTank, float shooterX,
                                                  float shooterY) {
        float targetX = profile.getLastX();
        float targetY = profile.getLastY();
        float targetVelX = profile.getVelocityX();
        float targetVelY = profile.getVelocityY();
        float bulletSpeed = shooterTank.getBulletSpeed();

        // distance
        float dx = targetX - shooterX;
        float dy = targetY - shooterY;
        float distanceToTarget = (float) Math.sqrt(dx * dx + dy * dy);

        // travel time
        float bulletTravelTime = distanceToTarget / bulletSpeed;

        // Predict where target will be at that time
        float predictedX = targetX + targetVelX * bulletTravelTime;
        float predictedY = targetY + targetVelY * bulletTravelTime;

        // Calculate confidence based on velocity freshness
        float confidence = calculateVelocityConfidence(profile);

        return new PredictedPosition(predictedX, predictedY, confidence);
    }

    /**
     * Calculate confidence in velocity predictions
     */
    private float calculateVelocityConfidence(AIMemory.EnemyProfile profile) {
        if (profile.getLastSeenTime() == 0) return DEFAULT_CONFIDENCE;

        long timeSinceLastSeen = System.currentTimeMillis() - profile.getLastSeenTime();
        float staleness = Math.min(1.0f, (float) timeSinceLastSeen / 1000);

        return 1.0f - (staleness * CONFIDENCE_DECAY);
    }

    public record PredictedPosition(float x, float y, float confidence) {
    }
}
