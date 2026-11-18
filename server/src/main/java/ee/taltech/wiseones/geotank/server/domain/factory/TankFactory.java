package ee.taltech.wiseones.geotank.server.domain.factory;

import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;

/**
 * Factory for creating tank instances.
 * Encapsulates tank initialization logic.
 */
public class TankFactory {

    /**
     * Creates a new player tank with default configuration.
     *
     * @param playerId the ID of the player who will own this tank
     * @return a fully initialized Tank
     */
    public Tank createPlayerTank(int playerId) {
        return new SimpleTank(playerId);
    }
}
