package ee.taltech.wiseones.geotank.server.domain.service.powerlevelsystem;

import ee.taltech.wiseones.geotank.server.domain.model.classupgrade.ClassUpgradeState;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.util.HashMap;
import java.util.Map;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Applies class-specific weight modifiers to stat weights.
 * Each class modifies certain continuous stats:
 * - Artillery: +Cannons (discrete), +Spread (discrete)
 * - Speedster: +Move Speed, +Bullet Speed
 * - Machine Gun: +Fire Rate, -Bullet Lifetime
 * - Tank: +Max HP, +HP Regen, -Move Speed
 * - Stream: +Bullets (discrete), -Bullet Speed, -Spread
 * - Rammer: +Body Damage, +Max HP
 * - Sniper: +Bullet Damage, +Pierce, -Fire Rate, +Bullet Lifetime
 */
public class WeightManager {
    private static final String KEY_MOVE_SPEED = "moveSpeed";
    private static final String KEY_BULLET_SPEED = "bulletSpeed";
    private static final String KEY_BULLET_LIFETIME = "bulletLifetime";
    private static final String KEY_MAX_HEALTH = "maxHealth";
    private static final String KEY_HEALTH_REGEN = "healthRegen";
    private static final String KEY_BODY_DAMAGE = "bodyDamage";
    private static final String KEY_PIERCE = "pierce";
    private static final String KEY_FIRE_RATE = "fireRate";
    private static final String KEY_BULLET_DAMAGE = "bulletDamage";

    /**
     * Apply weight modifiers for a class to stat weights.
     */
    public void applyClassWeights(TankClass tankClass, StatDistributor.StatWeights weights) {
        if (tankClass == TankClass.DEFAULT) {
            return;
        }

        switch (tankClass) {
            case ARTILLERY -> applyArtilleryWeights();
            case SPEEDSTER -> applySpeedsterWeights(weights);
            case MACHINE_GUN -> applyMachineGunWeights(weights);
            case TANK -> applyTankWeights(weights);
            case STREAM -> applyStreamWeights(weights);
            case RAMMER -> applyRammerWeights(weights);
            case SNIPER -> applySniperWeights(weights);
            default -> Logger.warn("Unknown tank class: " + tankClass);
        }
    }

    private void applyArtilleryWeights() {
        // Artillery: Discrete cannons and spread (no continuous weight changes)
        // Cannons and spread are handled separately as discrete upgrades
    }

    private void applySpeedsterWeights(StatDistributor.StatWeights weights) {
        // Speedster: +Move Speed, +Bullet Speed
        weights.moveSpeed += CLASS_SPEEDSTER_MOVE_SPEED_MOD;
        weights.bulletSpeed += CLASS_SPEEDSTER_BULLET_SPEED_MOD;
    }

    private void applyMachineGunWeights(StatDistributor.StatWeights weights) {
        // Machine Gun: +Fire Rate, -Bullet Lifetime
        weights.fireRate += CLASS_MACHINEGUN_FIRE_RATE_MOD;
        weights.bulletLifetime = weights.bulletLifetime + CLASS_MACHINEGUN_BULLET_LIFETIME_MOD;
    }

    private void applyTankWeights(StatDistributor.StatWeights weights) {
        // Tank: +Max HP, +HP Regen, -Move Speed
        weights.health += CLASS_TANK_HEALTH_MOD;
        weights.hpRegen += CLASS_TANK_REGEN_MOD;
        weights.moveSpeed = weights.moveSpeed + CLASS_TANK_MOVE_SPEED_MOD;
    }

    private void applyStreamWeights(StatDistributor.StatWeights weights) {
        // Stream: -Bullet Speed
        weights.bulletSpeed = weights.bulletSpeed + CLASS_STREAM_BULLET_SPEED_MOD;
    }

    private void applyRammerWeights(StatDistributor.StatWeights weights) {
        // Rammer: +Body Damage, +Max HP
        weights.bodyDamage += CLASS_RAMMER_BODY_DAMAGE_MOD;
        weights.health += CLASS_RAMMER_HEALTH_MOD;
    }

    private void applySniperWeights(StatDistributor.StatWeights weights) {
        // Sniper: +Bullet Damage, +Pierce, -Fire Rate, +Bullet Lifetime
        weights.bulletDamage += CLASS_SNIPER_BULLET_DAMAGE_MOD;
        weights.pierce += CLASS_SNIPER_PIERCE_MOD;
        weights.fireRate = weights.fireRate + CLASS_SNIPER_FIRE_RATE_MOD;
        weights.bulletLifetime += CLASS_SNIPER_BULLET_LIFE_MOD;
    }

    /**
     * Convert ClassUpgradeState's Map format to StatWeights format.
     */
    public StatDistributor.StatWeights convertToStatWeights(Map<String, Float> stateWeights) {
        StatDistributor.StatWeights weights = new StatDistributor.StatWeights();

        weights.moveSpeed = stateWeights.getOrDefault(KEY_MOVE_SPEED, weights.moveSpeed);
        weights.bulletSpeed = stateWeights.getOrDefault(KEY_BULLET_SPEED, weights.bulletSpeed);
        weights.bulletLifetime = stateWeights.getOrDefault(KEY_BULLET_LIFETIME, weights.bulletLifetime);
        weights.health = stateWeights.getOrDefault(KEY_MAX_HEALTH, weights.health);
        weights.hpRegen = stateWeights.getOrDefault(KEY_HEALTH_REGEN, weights.hpRegen);
        weights.bodyDamage = stateWeights.getOrDefault(KEY_BODY_DAMAGE, weights.bodyDamage);
        weights.pierce = stateWeights.getOrDefault(KEY_PIERCE, weights.pierce);
        weights.fireRate = stateWeights.getOrDefault(KEY_FIRE_RATE, weights.fireRate);
        weights.bulletDamage = stateWeights.getOrDefault(KEY_BULLET_DAMAGE, weights.bulletDamage);

        return weights;
    }

    /**
     * Convert StatWeights format back to ClassUpgradeState's Map format.
     */
    public Map<String, Float> convertFromStatWeights(StatDistributor.StatWeights w) {
        return new HashMap<>(Map.of(
                KEY_MOVE_SPEED, w.moveSpeed,
                KEY_BULLET_SPEED, w.bulletSpeed,
                KEY_BULLET_LIFETIME, w.bulletLifetime,
                KEY_MAX_HEALTH, w.health,
                KEY_HEALTH_REGEN, w.hpRegen,
                KEY_BODY_DAMAGE, w.bodyDamage,
                KEY_PIERCE, w.pierce,
                KEY_FIRE_RATE, w.fireRate,
                KEY_BULLET_DAMAGE, w.bulletDamage
        ));
    }

    /**
     * Save decayed weights back to ClassUpgradeState.
     */
    public void saveStatWeights(StatDistributor.StatWeights weights, ClassUpgradeState classUpgradeState) {
        Map<String, Float> converted = convertFromStatWeights(weights);
        classUpgradeState.setStatWeights(converted);
    }
}
