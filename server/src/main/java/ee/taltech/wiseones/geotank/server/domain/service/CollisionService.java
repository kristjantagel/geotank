package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;

import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Rect;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Vector2;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;

import java.util.Collection;
import java.util.List;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.HALF_FLOAT;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.TANK_BOUNCE_COEFFICIENT;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.TANK_BOUNCE_IMPULSE;

public class CollisionService {
    private final Rect gameBounds;

    public CollisionService(Rect gameBounds) {
        this.gameBounds = gameBounds;
    }

    /**
     *
     * @return true if the projectile collided with something or exited the game field
     */
    public boolean isColliding(Projectile projectile, Collection<Player> players) {
        Rect projRect = projectile.getBoundingBox();

        // 1. Boundary check (Early exit)
        if (!gameBounds.contains(projRect)) {
            return true;
        }

        // Iterate all players — pierce can hit multiple per tick. isConsumeHit() decrements per call.
        boolean consumed = false;
        for (Player player : players) {
            if (consumed) break;

            Tank tank = player.getTank();

            // Skip dead players and the owner
            if (player.getLives() <= 0 || tank == projectile.getOwner()) continue;

            Rect tankRect = tank.getBoundingBox();
            if (!projRect.isIntersecting(tankRect)) continue;
            if (projectile.hasAlreadyHit(player)) continue;

            projectile.markHit(player);
            int damage = projectile.getDamage();
            tank.applyHit(damage);
            player.setLives(Math.max(0, player.getLives() - damage));
            player.setLastHitByTank(projectile.getOwner());

            if (projectile.isConsumeHit()) {
                consumed = true;
            }
        }

        return consumed;
    }

    /**
     * Resolve tank-vs-tank collisions. Overlapping alive tanks are pushed apart
     * along the smaller-overlap axis (split 50/50) and their velocity on that axis
     * is zeroed.
     */
    public void resolveTankTankCollisions(List<Player> players) {
        long now = System.currentTimeMillis();
        int n = players.size();

        for (int i = 0; i < n; i++) {
            Player playerA = players.get(i);
            if (!playerA.isAlive()) continue;

            for (int j = i + 1; j < n; j++) {
                Player playerB = players.get(j);

                if (playerB.isAlive() &&
                        playerA.getTank().getBoundingBox().isIntersecting(playerB.getTank().getBoundingBox())) {
                    handleTankCollisionPair(playerA, playerB);
                    applyContactDamage(playerA, playerB, now);
                    applyContactDamage(playerB, playerA, now);
                }
            }
        }
    }

    /**
     * Orchestrates the collision resolution between two moving tanks.
     * This method determines the axis of least penetration (the 'separation axis')
     * and delegates the physical response to {@link #resolveAxisCollision}.
     *
     * @param pA The first player involved in the collision.
     * @param pB The second player involved in the collision.
     */
    private void handleTankCollisionPair(Player pA, Player pB) {
        Tank tA = pA.getTank();
        Tank tB = pB.getTank();
        Rect rA = tA.getBoundingBox();
        Rect rB = tB.getBoundingBox();

        float overlapX = calculateOverlap(rA.x(), rA.width(), rB.x(), rB.width());
        float overlapY = calculateOverlap(rA.y(), rA.height(), rB.y(), rB.height());

        PhysicsState psA = tA.getPhysicsState();
        PhysicsState psB = tB.getPhysicsState();

        if (overlapX < overlapY) {
            resolveAxisCollision(psA, psB, overlapX, true);
        } else {
            resolveAxisCollision(psA, psB, overlapY, false);
        }
    }

    /**
     * Push alive tanks out of any destructible they overlap. Destructibles are
     * immovable — only the tank moves. Tank velocity on the collision axis is
     * reflected with TANK_BOUNCE_COEFFICIENT so it feels like bumping off, not
     * sticking.
     */
    public void resolveTankDestructibleCollisions(List<Player> players, Collection<Destructible> destructibles) {
        if (destructibles.isEmpty()) return;
        long now = System.currentTimeMillis();

        for (Player player : players) {
            if (!player.isAlive()) continue;

            Tank tank = player.getTank();
            Rect tRect = tank.getBoundingBox();

            for (Destructible dest : destructibles) {
                Rect dRect = dest.getBounds();

                if (tRect.isIntersecting(dRect)) {
                    applyStaticCollision(tank.getPhysicsState(), tRect, dRect);
                    applyDestructibleContactDamage(player, dest, now);
                }
            }
        }
    }

    /** Body damage from contact with a destructible. Damage = dest.getHealth(); cooldown shared with tank-vs-tank. */
    private void applyDestructibleContactDamage(Player victim, Destructible dest, long now) {
        if (!victim.canTakeContactDamage(now)) return;
        int damage = dest.getHealth();
        if (damage <= 0) return;
        victim.takeDamage(damage);
        victim.recordContactDamage();
    }

    /**
     * Resolves a collision between a dynamic tank and an immovable destructible object.
     * The tank is projected out of the destructible's bounds along the shortest overlap
     * vector, and its velocity is reflected based on {@code TANK_BOUNCE_COEFFICIENT}.
     *
     * @param ps     The {@link PhysicsState} of the tank to be moved.
     * @param tRect  The bounding box of the tank.
     * @param dRect  The bounding box of the static destructible object.
     */
    private void applyStaticCollision(PhysicsState ps, Rect tRect, Rect dRect) {
        float overlapX = calculateOverlap(tRect.x(), tRect.width(), dRect.x(), dRect.width());
        float overlapY = calculateOverlap(tRect.y(), tRect.height(), dRect.y(), dRect.height());

        if (overlapX < overlapY) {
            float sign = tRect.x() < dRect.x() ? -1f : 1f;
            ps.setX(ps.getX() + sign * overlapX);
            ps.setSpeed(new Vector2(-TANK_BOUNCE_COEFFICIENT * ps.getSpeed().x(), ps.getSpeed().y()));
        } else {
            float sign = tRect.y() < dRect.y() ? -1f : 1f;
            ps.setY(ps.getY() + sign * overlapY);
            ps.setSpeed(new Vector2(ps.getSpeed().x(), -TANK_BOUNCE_COEFFICIENT * ps.getSpeed().y()));
        }
    }

    /**
     * Calculates the 1D overlap distance between two projected segments on a single axis.
     * Used to determine penetration depth for the Separating Axis Theorem (SAT).
     *
     * @param posA The starting coordinate of the first segment.
     * @param dimA The length (width or height) of the first segment.
     * @param posB The starting coordinate of the second segment.
     * @param dimB The length (width or height) of the second segment.
     * @return The float value representing the depth of the overlap.
     */
    private float calculateOverlap(float posA, float dimA, float posB, float dimB) {
        return Math.min(posA + dimA, posB + dimB) - Math.max(posA, posB);
    }

    /**
     * Performs the final physics resolution for two tanks on a specific axis.
     * This handles both the positional correction (anti-interpenetration) and the
     * instantaneous change in velocity (impulse) using a 50/50 displacement split.
     *
     * @param psA     Physics state of the first tank.
     * @param psB     Physics state of the second tank.
     * @param overlap The depth of the intersection to resolve.
     * @param isX     True if resolving on the X-axis, false for the Y-axis.
     */
    private void resolveAxisCollision(PhysicsState psA, PhysicsState psB, float overlap, boolean isX) {
        float push = overlap * HALF_FLOAT;
        float posA = isX ? psA.getX() : psA.getY();
        float posB = isX ? psB.getX() : psB.getY();
        float signA = posA < posB ? -1f : 1f;

        // Push apart
        if (isX) {
            psA.setX(psA.getX() + signA * push);
            psB.setX(psB.getX() - signA * push);
        } else {
            psA.setY(psA.getY() + signA * push);
            psB.setY(psB.getY() - signA * push);
        }

        // Velocity / Impulse math
        float vA = isX ? psA.getSpeed().x() : psA.getSpeed().y();
        float vB = isX ? psB.getSpeed().x() : psB.getSpeed().y();

        float common = ((1f - TANK_BOUNCE_COEFFICIENT) * vA + (1f + TANK_BOUNCE_COEFFICIENT) * vB) * HALF_FLOAT;
        float diff = ((1f + TANK_BOUNCE_COEFFICIENT) * vA + (1f - TANK_BOUNCE_COEFFICIENT) * vB) * HALF_FLOAT;

        float newVA = common + signA * TANK_BOUNCE_IMPULSE;
        float newVB = diff - signA * TANK_BOUNCE_IMPULSE;

        if (isX) {
            psA.setSpeed(new Vector2(newVA, psA.getSpeed().y()));
            psB.setSpeed(new Vector2(newVB, psB.getSpeed().y()));
        } else {
            psA.setSpeed(new Vector2(psA.getSpeed().x(), newVA));
            psB.setSpeed(new Vector2(psB.getSpeed().x(), newVB));
        }
    }

    private void applyContactDamage(Player victim, Player attacker, long now) {
        if (!victim.canTakeContactDamage(now)) return;
        int damage = attacker.getEffectiveContactDamage();
        victim.takeDamage(damage);
        victim.recordContactDamage();
        victim.setLastHitByTank(attacker.getTank());
    }

    public void collisionWithBounds(Tank tank) {
        PhysicsState state = tank.getPhysicsState();
        Rect bounds = tank.getBoundingBox();

        float x = state.getX();
        float y = state.getY();

        Vector2 v = state.getSpeed();
        Vector2 a = state.getAcceleration();

        boolean collidedX = false;
        boolean collidedY = false;

        // --- Left boundary ---
        if (bounds.x() < gameBounds.x()) {
            x = gameBounds.x() + bounds.width() * HALF_FLOAT;
            collidedX = true;
        }

        // --- Right boundary ---
        if (bounds.x() + bounds.width() > gameBounds.x() + gameBounds.width()) {
            x = gameBounds.x() + gameBounds.width() - bounds.width() * HALF_FLOAT;
            collidedX = true;
        }

        // --- Bottom boundary ---
        if (bounds.y() < gameBounds.y()) {
            y = gameBounds.y() + bounds.height() * HALF_FLOAT;
            collidedY = true;
        }

        // --- Top boundary ---
        if (bounds.y() + bounds.height() > gameBounds.y() + gameBounds.height()) {
            y = gameBounds.y() + gameBounds.height() - bounds.height() * HALF_FLOAT;
            collidedY = true;
        }

        // Apply corrected position
        if (collidedX) {
            state.setX(x);
            v = new Vector2(0, v.y());
            a = new Vector2(0, a.y());
        }
        if (collidedY) {
            state.setY(y);
            v = new Vector2(v.x(), 0);
            a = new Vector2(a.x(), 0);
        }
        if (collidedX || collidedY) {
            state.setSpeed(v);
            state.setAcceleration(a);
        }
    }
}
