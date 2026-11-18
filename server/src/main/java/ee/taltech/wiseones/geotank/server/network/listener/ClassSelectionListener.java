package ee.taltech.wiseones.geotank.server.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.LobbyManager;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.service.TankClassService;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.message.ClassSelectionMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Listener for tank class selection messages from clients.
 * Processes player's class choices when they reach level 5 and applies the upgrade.
 */
public class ClassSelectionListener extends Listener {
    private final LobbyManager lobbyManager;
    private final TankClassService tankClassService;

    public ClassSelectionListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
        this.tankClassService = new TankClassService();
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof ClassSelectionMessage classSelectMessage) {
            handleClassSelection(connection.getID(), classSelectMessage);
        }
        super.received(connection, object);
    }

    private void handleClassSelection(int connectionId, ClassSelectionMessage message) {
        TankClass primary = message.getPrimaryClass();
        TankClass secondary = message.getSecondaryClass();

        if (primary == null) {
            Logger.warn("Class selection from connection " + connectionId + " has null primary — ignoring");
            return;
        }

        Logger.info("Player at connection " + connectionId + " selected class: "
                + primary.getDisplayName()
                + (secondary != null ? " + " + secondary.getDisplayName() : ""));

        GameWorld gameWorld = lobbyManager.getGameWorldForConnection(connectionId);
        if (gameWorld == null) {
            Logger.warn("Ignoring class selection from connection " + connectionId + " — not in a game");
            return;
        }

        Player player = gameWorld.getPlayerManager().getPlayerByConnectionId(connectionId);
        if (player == null) {
            Logger.warn("Could not find player for connection " + connectionId);
            return;
        }

        // Verify player needs class selection
        if (!player.isNeedsClassSelection()) {
            Logger.warn("Player " + player.getId() + " attempted class selection but doesn't need it");
            return;
        }

        tankClassService.applyClassUpgrade(player, primary, secondary);

        // Force a stats broadcast so the client sees the new class
        // without waiting for the next XP gain.
        gameWorld.getPlayerManager().getPlayersWithUpdates().add(player.getId());

        Logger.info("Class upgrade applied to player " + player.getId());
    }
}
