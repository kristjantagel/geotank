package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.service.powerlevelsystem.ProceduralClassUpgradeSystem;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;

/**
 * Service for applying tank class upgrades using the new procedural weight-based system.
 * Delegates to ProceduralClassUpgradeSystem for the actual PL calculations.
 */
public class TankClassService {
    private final ProceduralClassUpgradeSystem upgradeSystem = new ProceduralClassUpgradeSystem();

    /**
     * Apply a class upgrade to a player.
     *
     * @param player the player receiving the upgrade
     * @param primaryClass the primary class to apply (required)
     * @param secondaryClass optional secondary class for multiclassing
     */
    public void applyClassUpgrade(Player player, TankClass primaryClass, TankClass secondaryClass) {
        upgradeSystem.applyClassUpgrade(player, primaryClass, secondaryClass);

        // Apply skill effects on top of class stats
        player.applySkillEffects();
    }
}


