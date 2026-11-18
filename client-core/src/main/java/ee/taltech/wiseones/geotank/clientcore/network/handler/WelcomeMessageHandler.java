package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.WelcomeMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Handles WelcomeMessage received from the server.
 * This is the first message a client receives after joining the game,
 * containing the player's ID, tank ID, and configuration for other players.
 */
public class WelcomeMessageHandler implements ClientMessageHandler<WelcomeMessage> {
    private final GameStateManager gameStateManager;

    public WelcomeMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(WelcomeMessage message) {
        Logger.info("Received welcome message: playerId=" + message.getPlayerId() +
                ", tankId=" + message.getTankId() +
                ", playerCount=" + message.getPlayerTankConfigs().size());
        gameStateManager.setWelcomeMessage(message);
    }
}
