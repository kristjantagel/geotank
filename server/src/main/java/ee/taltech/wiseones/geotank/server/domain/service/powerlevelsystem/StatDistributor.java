package ee.taltech.wiseones.geotank.server.domain.service.powerlevelsystem;

import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Distributes stat increases using bisection search to hit a target Power Level.
 * Uses the formula: newStat = currentStat + baseStat * (weightStat - 1) * x
 * Where x is found via binary search to match target PL within tolerance.
 */
public class StatDistributor {
    private final PowerLevelCalculator plCalculator = new PowerLevelCalculator();

    /**
     * Stat weight values used to scale distribution.
     * Keys: "health", "moveSpeed", "bulletSpeed", "bulletLifetime", "fireRate",
     *       "bulletDamage", "pierce", "bodyDamage", "hpRegen"
     */
    public static class StatWeights {
        float health = STARTING_WEIGHT;
        float moveSpeed = STARTING_WEIGHT;
        float bulletSpeed = STARTING_WEIGHT;
        float bulletLifetime = STARTING_WEIGHT;
        float fireRate = STARTING_WEIGHT;
        float bulletDamage = STARTING_WEIGHT;
        float pierce = STARTING_WEIGHT;
        float bodyDamage = STARTING_WEIGHT;
        float hpRegen = STARTING_WEIGHT;

        public double getTotalLeverage() {
            return Math.abs(health - TARGET_WEIGHT)
                    + Math.abs(moveSpeed - TARGET_WEIGHT)
                    + Math.abs(bulletSpeed - TARGET_WEIGHT)
                    + Math.abs(bulletLifetime - TARGET_WEIGHT)
                    + Math.abs(fireRate - TARGET_WEIGHT)
                    + Math.abs(bulletDamage - TARGET_WEIGHT)
                    + Math.abs(pierce - TARGET_WEIGHT)
                    + Math.abs(bodyDamage - TARGET_WEIGHT)
                    + Math.abs(hpRegen - TARGET_WEIGHT);
        }

        public void addDefaultGrowth() {
            health += DEFAULT_GROWTH;
            moveSpeed += DEFAULT_GROWTH;
            bulletSpeed += DEFAULT_GROWTH;
            bulletLifetime += DEFAULT_GROWTH;
            fireRate += DEFAULT_GROWTH;
            bulletDamage += DEFAULT_GROWTH;
            pierce += DEFAULT_GROWTH;
            bodyDamage += DEFAULT_GROWTH;
            hpRegen += DEFAULT_GROWTH;
        }

        public void decayTowardTarget() {
            health = decayWeight(health);
            moveSpeed = decayWeight(moveSpeed);
            bulletSpeed = decayWeight(bulletSpeed);
            bulletLifetime = decayWeight(bulletLifetime);
            fireRate = decayWeight(fireRate);
            bulletDamage = decayWeight(bulletDamage);
            pierce = decayWeight(pierce);
            bodyDamage = decayWeight(bodyDamage);
            hpRegen = decayWeight(hpRegen);
        }

        private float decayWeight(float weight) {
            float delta = WEIGHT_DECAY_FACTOR * (TARGET_WEIGHT - weight);

            // If the change is larger than or equal to the distance to target,
            // or if the weight is already very close, snap to target.
            if (Math.abs(delta) >= Math.abs(TARGET_WEIGHT - weight)) {
                return TARGET_WEIGHT;
            }

            return weight + delta;
        }
    }

    /**
     * Distribute the remaining power budget across continuous stats using bisection search.
     * Applies stat increases to the tank.
     */
    public void distributeStatIncreases(Tank tank, StatWeights weights, int upgradeLevel) {
        if (!(tank instanceof SimpleTank simpleTank)) return;

        double targetPL = plCalculator.getTargetPowerLevel(upgradeLevel);

        // 1. Check for leverage - if weights are all at target, add default growth
        double totalLeverage = weights.getTotalLeverage();
        if (totalLeverage < MIN_LEVERAGE_THRESHOLD) {
            Logger.info("  [Leverage] Low leverage (%.2f), applying default growth".formatted(totalLeverage));
            weights.addDefaultGrowth();
        } else {
            Logger.info("  [Leverage] Sufficient leverage (%.2f)".formatted(totalLeverage));
        }

        // 2. Bisection search for the right stat multiplier X
        double statMultiplier = bisectionSearch(simpleTank, weights, targetPL);

        // 3. Apply the stat increases
        applyStatIncreases(simpleTank, weights, statMultiplier);

        // 4. Decay weights for next upgrade
        weights.decayTowardTarget();

        Logger.debug("Stat distribution complete: x=%.4f, targetPL=%.1f".formatted(statMultiplier, targetPL));
    }

    /**
     * Binary search to find the multiplier X that brings total PL closest to target.
     * Allows negative multipliers to reduce stats when discrete upgrades exceed budget.
     */
    private double bisectionSearch(SimpleTank simpleTank, StatWeights weights, double targetPL) {
        double lowX = MIN_STAT_MULTIPLIER_BISECTION;    // Allow negative multipliers
        double highX = MAX_STAT_MULTIPLIER_BISECTION;
        double midX = 0;

        for (int i = 0; i < BISECTION_ITERATIONS; i++) {
            midX = (lowX + highX) / 2;

            // Test PL with this multiplier
            double testPL = calculateTestPL(simpleTank, weights, midX);

            // Check convergence
            if (Math.abs(testPL - targetPL) < PL_TOLERANCE) {
                break;
            }

            // Adjust search range
            if (testPL < targetPL) {
                lowX = midX;  // Need more growth
            } else {
                highX = midX;  // Need less growth
            }
        }

        return midX;
    }

    /**
     * Calculate PL with a given stat multiplier, without actually modifying the simpleTank.
     */
    private double calculateTestPL(SimpleTank simpleTank, StatWeights weights, double multiplier) {
        // Create a test snapshot of stats
        SimpleTank testTank = createStatSnapshot(simpleTank);
        applyStatIncreases(testTank, weights, multiplier);
        return plCalculator.calculateTotalPL(testTank);
    }

    /**
     * Create a shallow copy of tank stats for testing.
     */
    private SimpleTank createStatSnapshot(SimpleTank original) {
        SimpleTank copy = new SimpleTank(original.getId());
        copy.setMaxHpPL(original.getMaxHpPL());
        copy.setMoveSpeedPL(original.getMoveSpeedPL());
        copy.setBulletSpeedPL(original.getBulletSpeedPL());
        copy.setBulletLifetime(original.getBulletLifetime());
        copy.setFireRatePL(original.getFireRatePL());
        copy.setBulletDamagePL(original.getBulletDamagePL());
        copy.setPiercePL(original.getPiercePL());
        copy.setBodyDamagePL(original.getBodyDamagePL());
        copy.setHpRegenPL(original.getHpRegenPL());
        copy.setCannonCount(original.getCannonCount());
        copy.setBulletsPerCannon(original.getBulletsPerCannon());
        return copy;
    }

    /**
     * Helper to compute the final stat value based on current value, formula modifier, and minimum constraint.
     */
    private float getNewStatValue(float currentValue, double baseStat, float weight, double multiplier) {
        float modifier = (float) (baseStat * (weight - 1.0) * multiplier);

        // Prevent sign inversion when weight is below 1 and multiplier is negative
        if (weight < 1.0f && multiplier < 0) {
            modifier = -1 * modifier;
        }
//        Logger.info(String.format("current %f, base %f, weight %f, mult %f, mod %f, new %f", currentValue, baseStat,
//                weight, multiplier, modifier, Math.max(MIN_STAT_VALUE, currentValue + modifier)))
        return Math.max(MIN_STAT_VALUE, currentValue + modifier);
    }

    /**
     * Apply stat increases using the weight-based formula.
     */
    private void applyStatIncreases(SimpleTank simpleTank, StatWeights weights, double multiplier) {
        simpleTank.setHpRegenPL(getNewStatValue(simpleTank.getHpRegenPL(), BASE_HP_REGEN, weights.hpRegen, multiplier));
        simpleTank.setMaxHpPL((int) getNewStatValue(simpleTank.getMaxHpPL(), BASE_HEALTH, weights.health, multiplier));
        simpleTank.setBodyDamagePL(getNewStatValue(simpleTank.getBodyDamagePL(),
                BASE_BODY_DAMAGE, weights.bodyDamage, multiplier));
        simpleTank.setBulletSpeedPL(getNewStatValue(simpleTank.getBulletSpeedPL(),
                BASE_BULLET_SPEED, weights.bulletSpeed, multiplier));
        simpleTank.setPiercePL(getNewStatValue(simpleTank.getPiercePL(),
                BASE_PIERCE, weights.pierce, multiplier));
        simpleTank.setBulletDamagePL(getNewStatValue(simpleTank.getBulletDamagePL(),
                BASE_BULLET_DAMAGE, weights.bulletDamage, multiplier));
        simpleTank.setFireRatePL(getNewStatValue(simpleTank.getFireRatePL(),
                BASE_FIRE_RATE, weights.fireRate, multiplier));
        simpleTank.setMoveSpeedPL(getNewStatValue(simpleTank.getMoveSpeedPL(),
                BASE_MOVE_SPEED, weights.moveSpeed, multiplier));
        simpleTank.setBulletLifetime(getNewStatValue(simpleTank.getBulletLifetime(),
                BASE_BULLET_LIFETIME, weights.bulletLifetime, multiplier));
    }
}
