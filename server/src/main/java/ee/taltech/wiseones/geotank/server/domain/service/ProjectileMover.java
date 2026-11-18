package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsProperties;
import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Vector2;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.HALF_FLOAT;

public class ProjectileMover {
    public void move(Projectile projectile, float deltaTimeMs) {
        PhysicsState state = projectile.getPhysicsState();
        PhysicsProperties props = projectile.getPhysicsProperties();

        Vector2 speed = state.getSpeed();
        Vector2 acceleration = state.getAcceleration();

        float deltaSeconds = deltaTimeMs / 1000;

        // position updates based on old speed + acceleration: x = x0 + v*t + 0.5*a*t^2
        float deltaX = speed.x() * deltaSeconds + HALF_FLOAT * acceleration.x() * deltaSeconds * deltaSeconds;
        float deltaY = speed.y() * deltaSeconds + HALF_FLOAT * acceleration.y() * deltaSeconds * deltaSeconds;

        state.setX(state.getX() + deltaX);
        state.setY(state.getY() + deltaY);

        // update speed: v = v + a*t
        Vector2 newSpeed = speed.add(acceleration.scale(deltaSeconds));

        // limit speed according to projectile's maxSpeed
        float maxSpeed = props.getMaxSpeed();
        newSpeed = newSpeed.limit(maxSpeed);

        state.setSpeed(newSpeed);
    }
}
