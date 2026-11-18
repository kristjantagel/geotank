package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.LobbyListMessage;

public class LobbyListMessageHandler implements ClientMessageHandler<LobbyListMessage> {
    private final GameStateManager gameStateManager;

    public LobbyListMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(LobbyListMessage message) {
        gameStateManager.setLobbyInfoList(message.getLobbies());
    }
}
