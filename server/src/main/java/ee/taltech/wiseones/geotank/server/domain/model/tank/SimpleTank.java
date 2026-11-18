package ee.taltech.wiseones.geotank.server.domain.model.tank;

import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsProperties;
import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Rect;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Vector2;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Shell;
import ee.taltech.wiseones.geotank.shared.message.dto.Direction;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

// REFACTOR IN THE FUTURE

@Getter
@Setter
public class SimpleTank implements Tank {
    public static final float CIRCLE_DEGREES = 360f;
    public static final float MULTIPLICATIVE_BULLET_SPREAD = 0.25f;
    private final int id;

    private final PhysicsProperties physicsProperties;
    private final PhysicsState physicsState;
    private Direction desiredDirection = null;
    private float facingAngle = 0;
    private boolean wantsToShoot;

    private long lastShotFired = 0;
    private boolean fireAlternatingCannon = false;  // Alternates which cannon fires for x.5 bullets
    private int directionMask = 0; // Bitmask for diagonal movement (UP=1, DOWN=2, LEFT=4, RIGHT=8)

    // DON'T USE OUTSIDE PL calculations
    private float hpRegenPL = BASE_HP_REGEN;
    private int maxHpPL = BASE_HEALTH;
    private float bodyDamagePL = BASE_BODY_DAMAGE;
    private float bulletSpeedPL = BASE_BULLET_SPEED;
    private float piercePL = BASE_PIERCE;
    private float bulletDamagePL = BASE_BULLET_DAMAGE;
    private float fireRatePL = BASE_FIRE_RATE;
    private float moveSpeedPL = BASE_MOVE_SPEED;

    // USE THESE VALUES FOR GAME BEHAVIOUR
    // TYPE in the comments shows applied value type (float bodyDamage -> int when bodyDamage is applied)
    // This is for continuous scaling
    private float bulletLifetime = BASE_BULLET_LIFETIME;                            // Affected only by PL
    private int cannonCount = BASE_CANNONS;                                         // Affected only by PL
    private float bulletsPerCannon = BASE_BULLETS;                                  // Affected only by PL
    private float spreadAngle = BASE_SPREAD;                                        // Affected only by PL
    private float hpRegen = (float) BASE_HP_REGEN / 10;                             // Additive       FLOAT Skill + PL
    private int maxHp = BASE_HEALTH;                                                // Additive INT   Skill + PL
    private float bodyDamage = BASE_BODY_DAMAGE;                                    // Additive       INT   Skill + PL
    private float bulletSpeed = BASE_BULLET_SPEED;                                  // Multiplicative FLOAT Skill + PL
    private float pierce = BASE_PIERCE;                                             // Additive       INT   Skill + PL
    private int bulletDamage = (int) BASE_BULLET_DAMAGE/ 10;                        // Additive       INT   Skill + PL
    private long fireRateCooldownMs = (long) (1000 / BASE_FIRE_RATE);               // Multiplicative LONG  Skill + PL
    private float moveSpeed = BASE_MOVE_SPEED;                                      // Multiplicative FLOAT Skill + PL
    private float acceleration = BASE_MOVE_SPEED * ACCELERATION_MULTIPLIER;         // Derived from moveSpeed

    private Vector2 recoilVector = new Vector2(0, 0);

    public SimpleTank(int id) {
        this.id = id;
        // Initialize with base game stats (no class modifiers)
        physicsProperties = new PhysicsProperties(
                TANK_WIDTH,
                TANK_HEIGHT,
                TANK_PHYSICS_MAX_SPEED
        );
        physicsState = new PhysicsState(0, 0, new Vector2(0, 0), new Vector2(0, 0));
    }

    public List<Projectile> fire() {
        List<Projectile> projectiles = new ArrayList<>();

        int baseBullets = (int) bulletsPerCannon;
        float fractional = bulletsPerCannon - baseBullets;
        boolean useExtraBullet = fractional >= CLASS_STREAM_BULLETS_MOD && fireAlternatingCannon;
        int bullets = baseBullets + (useExtraBullet ? 1 : 0);

        for (int cannonIdx = 0; cannonIdx < cannonCount; cannonIdx++) {
            for (int bulletIdx = 0; bulletIdx < bullets; bulletIdx++) {
                Projectile projectile = fireFromCannon(cannonIdx, bulletIdx, bullets);
                projectiles.add(projectile);
            }
        }

        if (fractional >= CLASS_STREAM_BULLETS_MOD) {
            fireAlternatingCannon = !fireAlternatingCannon;
        }

        Logger.debug(String.format("fired %d projectiles from %d cannons", projectiles.size(), cannonCount));
        return projectiles;
    }

    /**
     * Fire a projectile from a specific cannon.
     * Cannon spread is applied via the tank's spread angle and bullet distribution.
     *
     * @param cannonIndex index of the cannon
     * @param bulletIndex index of the bullet inside this cannon's volley
     * @param bullets number of bullets this cannon fires this shot
     * @return a Projectile, or null if firing failed
     */
    private Projectile fireFromCannon(int cannonIndex, int bulletIndex, int bullets) {
        // Make sure x cannons on top of each other still shoot in slightly different angles for confirming to the
        // player that they are shooting >1 bullets, also it'd make extremely strong builds
        float effectiveSpread = Math.max(1f, spreadAngle);
        if (effectiveSpread * cannonCount >= CIRCLE_DEGREES) {
            effectiveSpread = CIRCLE_DEGREES / cannonCount;
        }
        float cannonOffset = 0f;
        if (cannonCount > 1) {
            float centerOffset = -((float) (cannonCount - 1) / 2) * effectiveSpread;
            cannonOffset = centerOffset + cannonIndex * effectiveSpread;
        }

        float bulletOffset = 0f;
        if (bullets > 1) {
            float bulletStep = effectiveSpread * MULTIPLICATIVE_BULLET_SPREAD;
            float bulletCenter = -((float) (bullets - 1) / 2) * bulletStep;
            bulletOffset = bulletCenter + bulletIndex * bulletStep;
        }

        float shotAngle = cannonOffset + bulletOffset;
        // Combine PL-calculated damage with skill bonus
        return new Shell(this, shotAngle, this.bulletDamage);
    }

    public boolean canShoot() {
        return fireRateCooldownMs <= (System.currentTimeMillis() - lastShotFired);
    }

    public void markShotFired() {
        lastShotFired = System.currentTimeMillis();
    }

    @Override
    public Rect getBoundingBox() {
        return Rect.fromCenter(physicsState.getX(), physicsState.getY(),
                physicsProperties.getWidth(), physicsProperties.getHeight());
    }

    public void applyHit(int damage) {
        // HitEffect properties, reserved for future use
    }
}
