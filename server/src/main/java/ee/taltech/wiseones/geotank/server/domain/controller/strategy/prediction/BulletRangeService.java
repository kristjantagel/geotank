package ee.taltech.wiseones.geotank.server.domain.controller.strategy.prediction;

import ee.taltech.wiseones.geotank.server.domain.controller.strategy.util.MovementUtils;
import ee.taltech.wiseones.geotank.server.domain.model.Player;

/**
 * Calculates effective bullet range and whether targets are within reach.
 */
public class BulletRangeService {
    public static final float MAX_RANGE_MARGIN = 0.9f;

    /**
     * Calculate the maximum bullet range for a player based on their actual tank stats
     *
     * @param player the player whose range to calculate
     * @return maximum distance a bullet can travel
     */
    public float calculateMaxBulletRange(Player player) {
        if (player == null || player.getTank() == null) {
            return 100; // Safe default
        }

        // Use actual tank stats instead of estimates
        float bulletSpeed = player.getTank().getBulletSpeed();
        float bulletLifetime = player.getTank().getBulletLifetime();

        return bulletSpeed * (bulletLifetime / 1000);
    }

    /**
     * Check if a target is within effective bullet range
     *
     * @param player the attacking player
     * @param targetX target X position
     * @param targetY target Y position
     * @param aiX AI position
     * @param aiY AI position
     * @return true if target is within range with margin for error
     */
    public boolean isTargetInRange(Player player, float targetX, float targetY, float aiX, float aiY) {
        float maxRange = calculateMaxBulletRange(player);
        float distance = distance(aiX, aiY, targetX, targetY);
        float effectiveRange = maxRange * MAX_RANGE_MARGIN;

        return distance <= effectiveRange;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return MovementUtils.distance(x1, y1, x2, y2);
    }
}
