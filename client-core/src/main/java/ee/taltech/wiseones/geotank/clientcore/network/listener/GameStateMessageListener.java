package ee.taltech.wiseones.geotank.clientcore.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.GameLeaveMessage;
import ee.taltech.wiseones.geotank.shared.message.GameStartMessage;
import ee.taltech.wiseones.geotank.shared.message.GameStateMessage;
import ee.taltech.wiseones.geotank.shared.message.LobbyErrorMessage;
import ee.taltech.wiseones.geotank.shared.message.LobbyListMessage;
import ee.taltech.wiseones.geotank.shared.message.LobbyStateMessage;
import ee.taltech.wiseones.geotank.shared.message.PlayerTankConfigMessage;
import ee.taltech.wiseones.geotank.shared.message.WelcomeMessage;

/**
 * Listens to the network, forwards leaked messages to the info holder
 */
public class GameStateMessageListener extends Listener {

    private final GameStateManager gameStateManager;

    public GameStateMessageListener(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void disconnected(Connection connection) {
        super.disconnected(connection);
        gameStateManager.setDisconnected();
    }

    /**
     * Processes received GameStateMessage objects.
     *
     * @param connection Connection that sent the message.
     * @param object     Received object (in this case GameStateMessage).
     */
    @Override
    public void received(Connection connection, Object object) {
        super.received(connection, object);

        if (object instanceof GameStateMessage msg) {
            gameStateManager.setLatestGameStateMessage(msg);
        } else if (object instanceof WelcomeMessage msg) {
            gameStateManager.setWelcomeMessage(msg);
        } else if (object instanceof PlayerTankConfigMessage msg) {
            gameStateManager.setPlayerTankConfigMessage(msg);
        } else if (object instanceof GameLeaveMessage msg) {
            gameStateManager.setGameLeaveMessage(msg);
        } else if (object instanceof LobbyStateMessage msg) {
            gameStateManager.setLobbyPlayers(msg.getPlayers());
            gameStateManager.setCurrentLobbyName(msg.getLobbyName());
            gameStateManager.setCurrentLobbyCode(msg.getLobbyCode());
        } else if (object instanceof GameStartMessage) {
            gameStateManager.setGameStarting(true);
        } else if (object instanceof LobbyListMessage msg) {
            gameStateManager.setLobbyInfoList(msg.getLobbies());
        } else if (object instanceof LobbyErrorMessage msg) {
            gameStateManager.setLobbyError(msg.getReason());
        }
    }
}