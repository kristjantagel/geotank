package ee.taltech.wiseones.geotank.server.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.LobbyManager;
import ee.taltech.wiseones.geotank.server.domain.controller.Controller;
import ee.taltech.wiseones.geotank.server.domain.controller.NetworkController;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import ee.taltech.wiseones.geotank.shared.message.TankFacingMessage;
import ee.taltech.wiseones.geotank.shared.message.TankMovementMessage;

public class TankMovementListener extends Listener {
    private final LobbyManager lobbyManager;

    public TankMovementListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof TankMovementMessage tankMovementMessage) {
            handleMovement(connection.getID(), tankMovementMessage);
        } else if (object instanceof TankFacingMessage tankFacingMessage) {
            handleFacing(connection.getID(), tankFacingMessage);
        }

        super.received(connection, object);
    }

    private void handleMovement(int connectionId, TankMovementMessage tankMovementMessage) {
        int mask = tankMovementMessage.getDirectionMask() & 0xF;
        Logger.debug("Player " + connectionId + " moved: mask=" + Integer.toBinaryString(mask));

        GameWorld gameWorld = lobbyManager.getGameWorldForConnection(connectionId);
        if (gameWorld == null) {
            Logger.debug("Ignoring movement from connection " + connectionId + " — not in a game");
            return;
        }

        Player player = gameWorld.getPlayerManager().getPlayerByConnectionId(connectionId);
        if (player == null) return;

        Controller controller = player.getController();
        if (controller instanceof NetworkController netCtrl) {
            netCtrl.setDirectionMask(mask);
        }
    }

    private void handleFacing(int connectionId, TankFacingMessage tankFacingMessage) {
        float angle = tankFacingMessage.getAngleRad();
        if (!Float.isFinite(angle)) {
            Logger.warn("Rejecting non-finite facing angle from connection " + connectionId);
            return;
        }
        Logger.debug("Player " + connectionId + " rotated: " + angle);

        GameWorld gameWorld = lobbyManager.getGameWorldForConnection(connectionId);
        if (gameWorld == null) {
            Logger.debug("Ignoring facing from connection " + connectionId + " — not in a game");
            return;
        }

        Player player = gameWorld.getPlayerManager().getPlayerByConnectionId(connectionId);
        if (player == null) return;

        Controller controller = player.getController();
        if (controller instanceof NetworkController netCtrl) {
            netCtrl.setFacingAngle(angle);
        }
    }
}
