package ee.taltech.wiseones.geotank.server.domain.service.powerlevelsystem;

import ee.taltech.wiseones.geotank.server.domain.model.physics.Vector2;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Calculates Power Level (PL) for a tank using the three-component formula:
 * PL_total = PL_offense + PL_utility - PL_recoil
 */
public class PowerLevelCalculator {

    /**
     * Calculate the total Power Level of a tank.
     */
    public double calculateTotalPL(Tank tank) {
        double offensePL = calculateOffensePL(tank);
        double utilityPL = calculateUtilityPL(tank);
        double recoilPL = calculateRecoilPL(tank);

        return offensePL + utilityPL - recoilPL;
    }

    /**
     * Calculate offense component: α × (D × P × F × C × B)
     * D: Bullet Damage, P: Pierce, F: Fire Rate, C: Cannons, B: Bullets per Cannon
     */
    public double calculateOffensePL(Tank tank) {
        if (!(tank instanceof SimpleTank simpleTank)) return 0;

        float bulletDamage = simpleTank.getBulletDamagePL();
        float pierce = simpleTank.getPiercePL();
        float fireRate = simpleTank.getFireRatePL();
        int cannons = simpleTank.getCannonCount();
        float bulletsPerCannon = simpleTank.getBulletsPerCannon();

        double offensiveProduct = bulletDamage * pierce * fireRate * cannons * bulletsPerCannon;
        return PL_OFFENSE_ALPHA * offensiveProduct;
    }

    /**
     * Calculate utility component (additive):
     * (λ_hp × HP) + (λ_reg × Regen) + (λ_bd × BodyDmg) + (λ_ms × MoveSpeed) + (λ_bs × BulletSpeed) + (λ_lt × Lifetime)
     */
    public double calculateUtilityPL(Tank tank) {
        if (!(tank instanceof SimpleTank simpleTank)) return 0;

        double utilityPL = 0;
        utilityPL += PL_LAMBDA_HP * simpleTank.getMaxHpPL();
        utilityPL += PL_LAMBDA_REGEN * simpleTank.getHpRegenPL();
        utilityPL += PL_LAMBDA_BODY_DAMAGE * simpleTank.getBodyDamagePL();
        utilityPL += PL_LAMBDA_MOVE_SPEED * simpleTank.getMoveSpeedPL();
        utilityPL += PL_LAMBDA_BULLET_SPEED * simpleTank.getBulletSpeedPL();
        utilityPL += PL_LAMBDA_LIFETIME * simpleTank.getBulletLifetime();

        return utilityPL;
    }

    /**
     * Calculate recoil component: β × len(recoilVector), capped at MAX_RECOIL
     */
    public double calculateRecoilPL(Tank tank) {
        if (!(tank instanceof SimpleTank simpleTank)) return 0;

        Vector2 recoilVector = simpleTank.getRecoilVector();
        if (recoilVector == null) return 0;

        double recoilLength = recoilVector.magnitude();

        // Cap recoil vector length
        if (recoilLength > MAX_RECOIL) {
            Logger.debug("  [Recoil] CAPPED: %.2f -> %.2f (MAX_RECOIL=%.2f)".formatted(
                    recoilLength, MAX_RECOIL, MAX_RECOIL));
            recoilLength = MAX_RECOIL;
        }

        return PL_RECOIL_BETA * recoilLength;
    }

    /**
     * Get the target PL for a given player level/upgrade tier.
     * targetPL = BASE_POWER + (upgradeLevel * POWER_PER_LEVEL)
     */
    public double getTargetPowerLevel(int upgradeLevel) {
        return (double) BASE_POWER + (upgradeLevel * POWER_PER_LEVEL);
    }

    /**
     * Check if the offensive PL is greater than recoil PL (constraint must always hold).
     * Returns true if: PL_offense > PL_recoil
     */
    public boolean isOffenseGreaterThanRecoil(Tank tank) {
        // TOOO: get recoil properly balanced and then implement this check
        if (!(tank instanceof SimpleTank)) return true;

        double offensePL = calculateOffensePL(tank);
        double recoilPL = calculateRecoilPL(tank);

        return offensePL > recoilPL;
    }
}
