package ee.taltech.wiseones.geotank.server.domain.model.classupgrade;

import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Manages the procedural class upgrade system for a player.
 * Tracks class progression, stat weights, and power level budget.
 */
@Getter
@Setter
public class ClassUpgradeState {

    private TankClass primaryClass = TankClass.DEFAULT;
    private TankClass secondaryClass = null; // null for single class, set for multiclass

    // Continuous stat weights (all start at STARTING_WEIGHT)
    private Map<String, Float> statWeights;

    // Current upgrade level (increases every CLASS_UPGRADE_INTERVAL_LEVELS)
    private int upgradeLevel = 0;

    // Tracks last upgrade player level to know when next class upgrade is available
    private int lastClassUpgradeAtLevel = 0;

    public ClassUpgradeState() {
        this.statWeights = initializeWeights();
    }

    public void reset() {
        primaryClass = TankClass.DEFAULT;
        secondaryClass = null;
        statWeights = initializeWeights();
        upgradeLevel = 0;
        lastClassUpgradeAtLevel = 0;
    }

    /**
     * Initialize all continuous stat weights to STARTING_WEIGHT.
     */
    private static Map<String, Float> initializeWeights() {
        Map<String, Float> weights = new HashMap<>();
        weights.put("moveSpeed", STARTING_WEIGHT);
        weights.put("bulletSpeed", STARTING_WEIGHT);
        weights.put("bulletLifetime", STARTING_WEIGHT);
        weights.put("maxHealth", STARTING_WEIGHT);
        weights.put("healthRegen", STARTING_WEIGHT);
        weights.put("bodyDamage", STARTING_WEIGHT);
        weights.put("pierce", STARTING_WEIGHT);
        weights.put("fireRate", STARTING_WEIGHT);
        weights.put("bulletDamage", STARTING_WEIGHT);
        return weights;
    }
}
