package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.LobbyStateMessage;

public class LobbyStateMessageHandler implements ClientMessageHandler<LobbyStateMessage> {
    private final GameStateManager gameStateManager;

    public LobbyStateMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(LobbyStateMessage message) {
        // Set name + code FIRST, players LAST. The browser/create screens use a non-empty
        // lobbyPlayers list as the trigger to transition to LobbyScreen; publishing the
        // name and code before that trigger guarantees the new screen sees them on its
        // very first render instead of a blank "LOBBY" / "Public lobby" frame.
        gameStateManager.setCurrentLobbyName(message.getLobbyName());
        gameStateManager.setCurrentLobbyCode(message.getLobbyCode());
        gameStateManager.setLobbyPlayers(message.getPlayers());
    }
}
