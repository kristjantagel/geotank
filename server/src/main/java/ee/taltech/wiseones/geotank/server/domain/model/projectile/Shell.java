package ee.taltech.wiseones.geotank.server.domain.model.projectile;

import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsProperties;
import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Rect;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Vector2;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;
import ee.taltech.wiseones.geotank.shared.constant.Constants;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

public class Shell implements Projectile {
    public static final float BARREL_LENGTH = 15f;
    private static final Vector2 VECTOR_ZERO = new Vector2(0, 0);
    @Getter
    private final long spawnTime;
    @Getter
    private final PhysicsProperties physicsProperties;
    @Getter
    private final PhysicsState physicsState;
    @Getter
    private final Tank owner;
    @Getter
    private final int damage;
    private int hitsRemaining;
    private final Set<Object> hitTargets = new HashSet<>();

    public Shell(Tank tank, float spreadAngle, int damage) {
        this.owner = tank;
        this.damage = damage;
        this.spawnTime = System.currentTimeMillis();

        PhysicsState tankState = tank.getPhysicsState();

        // 1. Combine angle calculations
        float finalAngleRad = tank.getFacingAngle() + (float) Math.toRadians(spreadAngle);
        float cos = (float) Math.cos(finalAngleRad);
        float sin = (float) Math.sin(finalAngleRad);

        // 2. Get stats
        float bulletSpeed = 100;
        float pierce = 1;

        if (tank instanceof SimpleTank st) {
            bulletSpeed = st.getBulletSpeed();
            pierce = st.getPierce();
        }

        this.hitsRemaining = (int) pierce;

        // 3. Reuse Trig for both Position and Velocity
        float spawnX = tankState.getX() + cos * BARREL_LENGTH;
        float spawnY = tankState.getY() + sin * BARREL_LENGTH;

        // Inherit tank momentum + bullet thrust
        Vector2 tankVel = tankState.getSpeed();
        Vector2 bulletVelocity = new Vector2(
                cos * bulletSpeed + tankVel.x(),
                sin * bulletSpeed + tankVel.y()
        );

        // 4. Initialize Physics
        this.physicsProperties = new PhysicsProperties(
                Constants.BULLET_WIDTH_IN_PIXELS,
                Constants.BULLET_HEIGHT_IN_PIXELS,
                bulletSpeed
        );

        this.physicsState = new PhysicsState(
                spawnX,
                spawnY,
                bulletVelocity,
                VECTOR_ZERO
        );
    }

    @Override
    public Rect getBoundingBox() {
        return Rect.fromCenter(physicsState.getX(), physicsState.getY(),
                physicsProperties.getWidth(), physicsProperties.getHeight());
    }

    @Override
    public boolean isConsumeHit() {
        hitsRemaining--;
        return hitsRemaining <= 0;
    }

    @Override
    public boolean hasAlreadyHit(Object target) {
        return hitTargets.contains(target);
    }

    @Override
    public void markHit(Object target) {
        hitTargets.add(target);
    }
}
