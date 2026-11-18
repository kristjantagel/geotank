package ee.taltech.wiseones.geotank.server.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.LobbyManager;
import ee.taltech.wiseones.geotank.shared.message.RespawnRequestMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

public class RespawnListener extends Listener {
    private final LobbyManager lobbyManager;

    public RespawnListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof RespawnRequestMessage) {
            int connId = connection.getID();
            GameWorld gameWorld = lobbyManager.getGameWorldForConnection(connId);
            if (gameWorld == null) {
                Logger.debug("Ignoring respawn from connection " + connId + " — not in a game");
                return;
            }
            gameWorld.respawnPlayer(connId);
            Logger.debug("Respawn requested by connection " + connId);
        }
        super.received(connection, object);
    }
}
