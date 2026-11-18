package ee.taltech.wiseones.geotank.server.domain.controller;

import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import lombok.Setter;

public class NetworkController implements Controller {

    @Setter
    private int directionMask = 0; // Bitmask for diagonal movement support
    private boolean wantsToShoot = false;
    @Setter
    private float facingAngle = 0f;

    public void setWantsToShoot(boolean wantsToShoot) {
        Logger.debug("setWantsToShoot: " + wantsToShoot);
        this.wantsToShoot = wantsToShoot;
    }

    @Override
    public void update(Tank tank, float timeDeltaMs) {
        // Set the direction mask for movement service to use (supports diagonal movement)
        if (tank.getDirectionMask() != directionMask) {
            tank.setDirectionMask(directionMask);
        }

        if (tank.getFacingAngle() != facingAngle) {
            Logger.debug(String.format("Updating facing angle: %s", facingAngle));
            tank.setFacingAngle(facingAngle);
        }
        if (wantsToShoot) {
            tank.setWantsToShoot(true);
            wantsToShoot = false;
        }
    }
}
