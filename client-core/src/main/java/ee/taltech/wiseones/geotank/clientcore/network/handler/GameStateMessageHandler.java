package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.GameStateMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Handles GameStateMessage received from the server.
 * Updates the arena with current game state (tank and projectile positions).
 */
public class GameStateMessageHandler implements ClientMessageHandler<GameStateMessage> {
    private final GameStateManager gameStateManager;

    public GameStateMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(GameStateMessage message) {
        Logger.debug("Processing game state: " + message.getPlayerTankStates().size() + " tanks, " +
                message.getProjectileStates().size() + " projectiles");
        gameStateManager.setLatestGameStateMessage(message);
    }
}
