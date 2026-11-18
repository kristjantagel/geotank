package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.controller.AIController;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Service for automatically applying class upgrades to AI players.
 * When an AI player can upgrade their tank, randomly selects one of the 4 available classes.
 */
public class AIClassSelectionService {
    private final TankClassService tankClassService;

    public AIClassSelectionService() {
        this.tankClassService = new TankClassService();
    }

    /**
     * Checks if a player is an AI and needs class selection, then applies a random class.
     *
     * @param player the player to potentially upgrade
     */
    public void handleAIClassUpgrade(Player player) {
        // Only handle AI players
        if (!(player.getController() instanceof AIController)) {
            return;
        }

        // Only process players that need class selection
        if (!player.isNeedsClassSelection()) {
            return;
        }

        // Select a random class from available options
        TankClass selectedClass = selectRandomClass();
        Logger.info("AI Player " + player.getId() + " automatically selected class: " + selectedClass.getDisplayName());

        // Apply the selected class upgrade
        tankClassService.applyClassUpgrade(player, selectedClass, null);
    }

    /**
     * Randomly selects one of the available tank classes.
     *
     * @return a random TankClass
     */
    private TankClass selectRandomClass() {
        TankClass[] availableClasses = {
                TankClass.SPEEDSTER,
                TankClass.MACHINE_GUN,
                TankClass.TANK,
                TankClass.STREAM,
                TankClass.RAMMER,
                TankClass.SNIPER
        };
        int randomIndex = (int) (Math.random() * availableClasses.length);
        return availableClasses[randomIndex];
    }
}
