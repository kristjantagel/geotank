package ee.taltech.wiseones.geotank.server.domain.model.projectile;

import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsProperties;
import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Rect;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;

public interface Projectile {

    PhysicsProperties getPhysicsProperties();

    PhysicsState getPhysicsState();

    Rect getBoundingBox();

    Tank getOwner();

    long getSpawnTime();

    /**
     * Record a hit and decide whether this projectile should be removed.
     * Projectiles with bullet-penetration pass through multiple targets —
     * returns true only once the remaining hit budget is exhausted.
     */
    boolean isConsumeHit();

    int getDamage();

    /**
     * Returns true if this projectile has already hit the given target.
     */
    boolean hasAlreadyHit(Object target);

    /**
     * Mark the given target as having been hit by this projectile.
     */
    void markHit(Object target);
}
