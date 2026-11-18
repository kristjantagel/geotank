package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.PlayerTankConfigMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Handles PlayerTankConfigMessage received from the server.
 * Updates configuration for a player's tank (e.g., when a new player joins).
 */
public class PlayerTankConfigMessageHandler implements ClientMessageHandler<PlayerTankConfigMessage> {
    private final GameStateManager gameStateManager;

    public PlayerTankConfigMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(PlayerTankConfigMessage message) {
        var config = message.getPlayerTankConfig();
        Logger.debug("Received tank config: playerId=" + config.getPlayerId() + ", tankId=" + config.getTankId());
        gameStateManager.setPlayerTankConfigMessage(message);
    }
}
