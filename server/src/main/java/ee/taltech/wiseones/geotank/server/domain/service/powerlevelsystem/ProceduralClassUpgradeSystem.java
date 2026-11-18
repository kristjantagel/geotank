package ee.taltech.wiseones.geotank.server.domain.service.powerlevelsystem;

import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Vector2;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

public class ProceduralClassUpgradeSystem {
    private final PowerLevelCalculator plCalculator = new PowerLevelCalculator();
    private final StatDistributor statDistributor = new StatDistributor();
    private final WeightManager weightManager = new WeightManager();

    /**
     * Apply a class upgrade to a player.
     * Handles single-class and multiclass selections.
     * Flow:
     * 1. Load current weights from ClassUpgradeState (baseline for this upgrade)
     * 2. Apply class-specific weight modifiers
     * 3. Handle discrete upgrades (cannons, bullets)
     * 4. Use bisection search to distribute continuous stats
     * 5. Save decayed weights back to ClassUpgradeState
     * 6. Update primary/secondary classes
     */
    public void applyClassUpgrade(Player player, TankClass primaryClass, TankClass secondaryClass) {
        Tank tank = player.getTank();
        if (tank == null) {
            Logger.error("Cannot apply class upgrade - player has no tank");
            return;
        }

        if (!(tank instanceof SimpleTank simpleTank)) {
            Logger.error("Tank is not SimpleTank");
            return;
        }

        int playerLevel = player.getLevel();
        int upgradeLevel = player.getClassUpgrade().getUpgradeLevel();
        // Calculate target PL based on current tier, not upgradeLevel
        // At level 10-19: tier=1, targetPL=150; at level 20-29: tier=2, targetPL=200, etc.
        int currentTier = playerLevel / CLASS_UPGRADE_INTERVAL_LEVELS;
        double targetPL = plCalculator.getTargetPowerLevel(currentTier);

        // Capture stats before upgrade
        double plBefore = plCalculator.calculateTotalPL(tank);
        float regenBefore = simpleTank.getHpRegenPL();
        int healthBefore = simpleTank.getMaxHpPL();
        float bodyDmgBefore = simpleTank.getBodyDamagePL();
        float bsSpeedBefore = simpleTank.getBulletSpeedPL();
        float pierceBefore = simpleTank.getPiercePL();
        float bulletDmgBefore = simpleTank.getBulletDamagePL();
        float fireRateBefore = simpleTank.getFireRatePL();
        float msSpeedBefore = simpleTank.getMoveSpeedPL();
        int bulletLifetimeBefore = (int) simpleTank.getBulletLifetime();
        int cannonsBefore = simpleTank.getCannonCount();
        float bulletsBefore = simpleTank.getBulletsPerCannon();
        float angleBefore = simpleTank.getSpreadAngle();

        Logger.info("");
        Logger.info("=== CLASS UPGRADE: Player %d at Level %d (Upgrade Tier %d, Target PL=%.0f) ===".formatted(
                player.getId(),
                playerLevel,
                upgradeLevel,
                targetPL
        ));
        Logger.info("Selection: %s%s".formatted(
                primaryClass.getDisplayName(),
                secondaryClass != null ? " + " + secondaryClass.getDisplayName() : ""
        ));

        // 1. Load current weights from player's class upgrade state
        StatDistributor.StatWeights weightsBefore = weightManager.convertToStatWeights(
                player.getClassUpgrade().getStatWeights()
        );
        Logger.info("  Weights before modifiers: R=%.2f H=%.2f BD=%.2f BS=%.2f P=%.2f DMG=%.2f FR=%.2f MS=%.2f BL=%.2f"
                .formatted(
                        weightsBefore.hpRegen, weightsBefore.health, weightsBefore.bodyDamage,
                        weightsBefore.bulletSpeed, weightsBefore.pierce, weightsBefore.bulletDamage,
                        weightsBefore.fireRate, weightsBefore.moveSpeed, weightsBefore.bulletLifetime
                ));

        // 2. Apply class modifiers to weights
        StatDistributor.StatWeights weights = new StatDistributor.StatWeights();
        weights.hpRegen = weightsBefore.hpRegen;
        weights.health = weightsBefore.health;
        weights.bodyDamage = weightsBefore.bodyDamage;
        weights.bulletSpeed = weightsBefore.bulletSpeed;
        weights.pierce = weightsBefore.pierce;
        weights.bulletDamage = weightsBefore.bulletDamage;
        weights.fireRate = weightsBefore.fireRate;
        weights.moveSpeed = weightsBefore.moveSpeed;
        weights.bulletLifetime = weightsBefore.bulletLifetime;

        weightManager.applyClassWeights(primaryClass, weights);
        if (secondaryClass != null && secondaryClass != TankClass.DEFAULT) {
            weightManager.applyClassWeights(secondaryClass, weights);
        }
        Logger.info("  Weights after modifiers: R=%.2f H=%.2f BD=%.2f BS=%.2f P=%.2f DMG=%.2f FR=%.2f MS=%.2f BL=%.2f"
                .formatted(
                        weights.hpRegen, weights.health, weights.bodyDamage,
                        weights.bulletSpeed, weights.pierce, weights.bulletDamage,
                        weights.fireRate, weights.moveSpeed, weights.bulletLifetime
                ));

        // 3. Handle discrete upgrades (cannons, bullets, spread)
        handleDiscreteUpgrades(primaryClass, secondaryClass, simpleTank);

        // 4. Distribute continuous stat increases using bisection (also decays weights)
        statDistributor.distributeStatIncreases(tank, weights, currentTier);

        // 4b. Recalculate recoil vector based on new stats
        updateRecoilVector(simpleTank);

        // 5. Save decayed weights back to ClassUpgradeState
        weightManager.saveStatWeights(weights, player.getClassUpgrade());
        Logger.info("  Weights after decay: R=%.2f H=%.2f BD=%.2f BS=%.2f P=%.2f DMG=%.2f FR=%.2f MS=%.2f BL=%.2f"
                .formatted(
                        weights.hpRegen, weights.health, weights.bodyDamage,
                        weights.bulletSpeed, weights.pierce, weights.bulletDamage,
                        weights.fireRate, weights.moveSpeed, weights.bulletLifetime
                ));

        // 6. Update primary/secondary classes
        player.getClassUpgrade().setPrimaryClass(primaryClass);
        player.getClassUpgrade().setSecondaryClass(secondaryClass);
        player.getClassUpgrade().setLastClassUpgradeAtLevel(playerLevel);
        player.getClassUpgrade().setUpgradeLevel(upgradeLevel + 1);

        double plAfter = plCalculator.calculateTotalPL(tank);
        Logger.info("  PL: %.0f -> %.0f (target=%.0f, delta=%+.0f)".formatted(
                plBefore, plAfter, targetPL, plAfter - plBefore
        ));
        Logger.info((" Stats: Regen %.2f->%.2f, HP %d->%d, BDmg %.0f->%.0f, BS %.0f->%.0f, Pierce %.2f->%.2f," +
                "BDmg %.0f->%.0f, FR %.2f->%.2f, MS %.0f->%.0f, BLife %dms->%dms, Cannons %d->%d," +
                "Bullets %.1f->%.1f, Angle %.0f->%.0f")
                .formatted(
                        regenBefore, simpleTank.getHpRegenPL(),
                        healthBefore, simpleTank.getMaxHpPL(),
                        bodyDmgBefore, simpleTank.getBodyDamagePL(),
                        bsSpeedBefore, simpleTank.getBulletSpeedPL(),
                        pierceBefore, simpleTank.getPiercePL(),
                        bulletDmgBefore, simpleTank.getBulletDamagePL(),
                        fireRateBefore, simpleTank.getFireRatePL(),
                        msSpeedBefore, simpleTank.getMoveSpeedPL(),
                        bulletLifetimeBefore, (int) simpleTank.getBulletLifetime(),
                        cannonsBefore, simpleTank.getCannonCount(),
                        bulletsBefore, simpleTank.getBulletsPerCannon(),
                        angleBefore, simpleTank.getSpreadAngle()
                ));
    }

    /**
     * Handle discrete stat upgrades (cannons, bullets, spread).
     * These are defined in Constants and applied based on the selected classes.
     * Checks both primary and secondary classes for discrete upgrades.
     */
    private void handleDiscreteUpgrades(TankClass primary, TankClass secondary, SimpleTank simpleTank) {
        // Artillery: +Cannons, +Spread
        if (primary == TankClass.ARTILLERY || secondary == TankClass.ARTILLERY) {
            simpleTank.setCannonCount(simpleTank.getCannonCount() + CLASS_ARTILLERY_CANNON_MOD);
            simpleTank.setSpreadAngle(simpleTank.getSpreadAngle() + CLASS_ARTILLERY_SPREAD_MOD);
        }

        // Stream: +Bullets, -Bullet Speed, -Spread
        if (primary == TankClass.STREAM || secondary == TankClass.STREAM) {
            simpleTank.setBulletsPerCannon(simpleTank.getBulletsPerCannon() + CLASS_STREAM_BULLETS_MOD);
            simpleTank.setSpreadAngle(simpleTank.getSpreadAngle() + CLASS_STREAM_SPREAD_MOD);
        }
    }

    /**
     * Calculate and update the recoil vector based on simpleTank's current firing setup.
     * For each cannon position, simulate firing and sum the recoil vectors.
     * Recoil = -bulletDamage × sum(bullet_trajectory_unit_vectors)
     * Capped at MAX_RECOIL.
     */
    private void updateRecoilVector(SimpleTank simpleTank) {
        Vector2 totalRecoil = new Vector2(0, 0);

        float bulletsPerCannon = simpleTank.getBulletsPerCannon();
        if (simpleTank.getCannonCount() == 0 || bulletsPerCannon <= 0f) return;

        // Simulate directional contribution from each cannon using average bullet count.
        float tankAngle = simpleTank.getFacingAngle();
        float totalSpread = simpleTank.getSpreadAngle();

        for (int cannonIdx = 0; cannonIdx < simpleTank.getCannonCount(); cannonIdx++) {
            float cannonOffset = 0f;
            if (simpleTank.getCannonCount() > 1) {
                float center = -((float) (simpleTank.getCannonCount() - 1) / 2) * totalSpread;
                cannonOffset = center + cannonIdx * totalSpread;
            }

            float finalAngle = tankAngle + (float) Math.toRadians(cannonOffset);

            Vector2 bulletDir = new Vector2(
                    (float) Math.cos(finalAngle),
                    (float) Math.sin(finalAngle)
            );
            // impact = speed * count
            float cannonImpact = -simpleTank.getBulletSpeedPL() * bulletsPerCannon;
            Vector2 cannonRecoil = bulletDir.scale(cannonImpact);
            totalRecoil = totalRecoil.add(cannonRecoil);
        }

        // Cap recoil magnitude at MAX_RECOIL
        float recoilMag = totalRecoil.magnitude();
        if (recoilMag > MAX_RECOIL) {
            float scaleFactor = MAX_RECOIL / recoilMag;
            totalRecoil = totalRecoil.scale(scaleFactor);
        }

        simpleTank.setRecoilVector(totalRecoil);
        Logger.info("  [Recoil] Updated: magnitude=%.2f, capped=%s".formatted(
                recoilMag, recoilMag > MAX_RECOIL ? "yes" : "no"
        ));
    }
}
