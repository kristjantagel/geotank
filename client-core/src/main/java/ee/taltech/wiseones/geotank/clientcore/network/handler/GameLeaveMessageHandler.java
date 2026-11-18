package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.GameLeaveMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Handles GameLeaveMessage received from the server.
 * Notifies the client when another player leaves the game.
 */
public class GameLeaveMessageHandler implements ClientMessageHandler<GameLeaveMessage> {
    private final GameStateManager gameStateManager;

    public GameLeaveMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(GameLeaveMessage message) {
        Logger.info("Player " + message.getPlayerId() + " left the game");
        gameStateManager.setGameLeaveMessage(message);
    }
}
