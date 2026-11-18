package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.GameStartMessage;

public class GameStartMessageHandler implements ClientMessageHandler<GameStartMessage> {
    private final GameStateManager gameStateManager;

    public GameStartMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(GameStartMessage message) {
        gameStateManager.setGameStarting(true);
    }
}
