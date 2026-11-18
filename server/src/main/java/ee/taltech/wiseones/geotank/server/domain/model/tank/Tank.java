package ee.taltech.wiseones.geotank.server.domain.model.tank;

import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsProperties;
import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Rect;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.shared.message.dto.Direction;

import java.util.List;

public interface Tank {
    int getId();

    List<Projectile> fire();

    PhysicsProperties getPhysicsProperties();

    PhysicsState getPhysicsState();

    void setDesiredDirection(Direction direction);

    Direction getDesiredDirection();

    float getFacingAngle();

    void setFacingAngle(float angle);

    boolean canShoot();

    void markShotFired();

    void setWantsToShoot(boolean wantsToShoot);

    boolean isWantsToShoot();

    Rect getBoundingBox();

    void applyHit(int damage);

    int getDirectionMask();

    void setDirectionMask(int mask);

    float getAcceleration();

    float getMoveSpeed();

    float getBulletSpeed();

    float getBulletLifetime();
}
