package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;

import java.util.List;

public class ShootingService {
    /**
     * Called inside GameWorld.update() for each tank
     *
     * @param tank        tank that may shoot
     * @param projectiles GameWorld's projectile list, where new Projectile is added upon shooting
     */
    public void update(Tank tank, List<Projectile> projectiles) {
        // Does the tank want to shoot?
        if (!tank.isWantsToShoot()) return;
        // can the tank shoot
        if (!tank.canShoot()) return;

        // shooting itself
        List<Projectile> newProjectiles = tank.fire();
        if (newProjectiles != null && !newProjectiles.isEmpty()) {
            projectiles.addAll(newProjectiles);
            applyRecoil(tank);
        }

        // Mark shooting
        tank.markShotFired();
        tank.setWantsToShoot(false);
    }

    /**
     * Recoil is calculated with PL, static recoil is applied for computational efficiency.
     *
     * @param tank tank receiving the recoil
     */
    private void applyRecoil(Tank tank) {
        if (!(tank instanceof SimpleTank simpleTank)) return;

        // Apply the recoil impulse to the tank's velocity
        PhysicsState ps = tank.getPhysicsState();
        ps.setSpeed(ps.getSpeed().add(simpleTank.getRecoilVector()));
    }
}
