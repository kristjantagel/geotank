package ee.taltech.wiseones.geotank.server.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.LobbyManager;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.shared.message.SkillUpgradeMessage;
import ee.taltech.wiseones.geotank.shared.message.dto.SkillType;
import ee.taltech.wiseones.geotank.shared.util.Logger;

public class SkillUpgradeListener extends Listener {
    private final LobbyManager lobbyManager;

    public SkillUpgradeListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof SkillUpgradeMessage msg) {
            GameWorld gameWorld = lobbyManager.getGameWorldForConnection(connection.getID());
            if (gameWorld == null) return;

            Player player = gameWorld.getPlayerManager().getPlayerByConnectionId(connection.getID());
            if (player == null) return;

            int statIndex = msg.getStatIndex();
            SkillType[] types = SkillType.values();
            if (statIndex < 0 || statIndex >= types.length) return;

            if (player.isUpgradeSkill(types[statIndex])) {
                gameWorld.getPlayerManager().getPlayersWithUpdates().add(player.getId());
                Logger.debug("Player " + player.getId() + " upgraded " + types[statIndex]
                        + " to level " + player.getSkillLevel(types[statIndex]));
            }
        }

        super.received(connection, object);
    }
}
