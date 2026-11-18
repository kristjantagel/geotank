package ee.taltech.wiseones.geotank.server.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.LobbyManager;
import ee.taltech.wiseones.geotank.server.domain.controller.Controller;
import ee.taltech.wiseones.geotank.server.domain.controller.NetworkController;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import ee.taltech.wiseones.geotank.shared.message.PlayerShootingMessage;

public class TankShootingListener extends Listener {
    private final LobbyManager lobbyManager;

    public TankShootingListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof PlayerShootingMessage) {
            Logger.debug("Player " + connection.getID() + " shot");

            GameWorld gameWorld = lobbyManager.getGameWorldForConnection(connection.getID());
            if (gameWorld == null) {
                Logger.debug("Ignoring shot from connection " + connection.getID() + " — not in a game");
                return;
            }

            Player player = gameWorld.getPlayerManager().getPlayerByConnectionId(connection.getID());
            if (player == null) return;

            Controller controller = player.getController();
            if (controller instanceof NetworkController netCtrl) {
                netCtrl.setWantsToShoot(true);
            }
        }

        super.received(connection, object);
    }
}

