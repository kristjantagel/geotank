package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.message.ClassSelectionPromptMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Handles class selection prompts from the server.
 * When a player can upgrade their tank, displays available class options and enables input.
 */
public class ClassSelectionPromptMessageHandler implements ClientMessageHandler<ClassSelectionPromptMessage> {
    private final GameStateManager gameStateManager;

    public ClassSelectionPromptMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(ClassSelectionPromptMessage message) {
        TankClass[] availableClasses = message.getAvailableClasses();
        TankClass[] availableSecondaryClasses = message.getAvailableSecondaryClasses();

        Logger.info("=== CLASS SELECTION AVAILABLE ===");
        for (int i = 0; i < availableClasses.length; i++) {
            TankClass secondary = availableSecondaryClasses != null ? availableSecondaryClasses[i] : null;
            String label = (secondary == null)
                    ? availableClasses[i].getDisplayName()
                    : availableClasses[i].getDisplayName() + " + " + secondary.getDisplayName();
            Logger.info((i + 1) + ". " + label);
        }
        Logger.info("Press key 1-" + availableClasses.length + " to select your class");

        gameStateManager.setAvailableClasses(availableClasses);
        gameStateManager.setAvailableSecondaryClasses(availableSecondaryClasses);
        gameStateManager.setClassSelectionAvailable(true);
    }
}
