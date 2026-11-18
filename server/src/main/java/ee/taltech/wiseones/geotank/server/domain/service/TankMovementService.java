package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Vector2;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.shared.message.dto.Direction;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Force-based movement.
 * Physics Model:
 * Apply input force toward the current 2d movement vector
 * Apply static friction force opposing movement
 */
public class TankMovementService {

    public TankMovementService() {
        // Nothing to init right now
    }

    /**
     * Update tank movement for the given time delta with force-based physics
     */
    public void move(Tank tank, float deltaTimeMs) {
        PhysicsState state = tank.getPhysicsState();
        float deltaSeconds = deltaTimeMs / 1000.0f;

        Vector2 currentVelocity = state.getSpeed();

        // 1. Calculate input force using SimpleTank's acceleration
        Vector2 inputForce = calculateInputForce(tank);

        // 2. Calculate friction force
        Vector2 frictionForce = calculateFrictionForce(currentVelocity);

        // 3. Total acceleration
        Vector2 totalAcceleration = inputForce.add(frictionForce);

        // 4. Update velocity: v = v + a * dt
        Vector2 newVelocity = currentVelocity.add(totalAcceleration.scale(deltaSeconds));

        // 5. Enforce SimpleTank's moveSpeed limit
        float classMaxSpeed = tank.getMoveSpeed();
        newVelocity = newVelocity.limit(classMaxSpeed);

        // 6. Update position
        Vector2 displacement = newVelocity.scale(deltaSeconds);
        float newX = state.getX() + displacement.x();
        float newY = state.getY() + displacement.y();

        state.setX(newX);
        state.setY(newY);
        state.setSpeed(newVelocity);
        state.setAcceleration(totalAcceleration);
    }

    /**
     * Calculate input force based on direction bitmask.
     * Falls back to desiredDirection for AI/single-direction compatibility.
     */
    private Vector2 calculateInputForce(Tank tank) {
        float accel = tank.getAcceleration();
        int mask = tank.getDirectionMask();

        if (mask != 0) {
            float forceX = 0;
            float forceY = 0;

            if (Direction.hasUp(mask)) forceY += accel;
            if (Direction.hasDown(mask)) forceY -= accel;
            if (Direction.hasRight(mask)) forceX += accel;
            if (Direction.hasLeft(mask)) forceX -= accel;

            // Normalize diagonal force so tanks don't move faster diagonally
            Vector2 force = new Vector2(forceX, forceY);
            if (force.magnitude() > accel) {
                return force.limit(accel);
            }
            return force;
        }

        Direction direction = tank.getDesiredDirection();
        if (direction == null || direction == Direction.NONE) {
            return new Vector2(0, 0);
        }

        return switch (direction) {
            case UP -> new Vector2(0, accel);
            case DOWN -> new Vector2(0, -accel);
            case LEFT -> new Vector2(-accel, 0);
            case RIGHT -> new Vector2(accel, 0);
            default -> new Vector2(0, 0);
        };
    }

    /**
     * Calculate static friction force that opposes current velocity.
     * Friction deceleration is proportional to current speed, giving smooth stopping.
     */
    private Vector2 calculateFrictionForce(Vector2 velocity) {
        // Friction is a damping force: F_friction = -friction_coefficient * v
        // This creates smooth deceleration without sliding
        if (velocity.magnitude() < MIN_MOVEMENT) {
            return new Vector2(0, 0); // Stop completely when nearly stopped
        }

        // Apply friction as a percentage of current velocity
        return velocity.scale(-FRICTION_COEFFICIENT * velocity.magnitude());
    }
}
