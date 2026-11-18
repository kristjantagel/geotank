package ee.taltech.wiseones.geotank.server.domain.model;

import ee.taltech.wiseones.geotank.server.domain.model.physics.Rect;
import ee.taltech.wiseones.geotank.shared.message.dto.DestructibleType;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a destructible object in the game world.
 * Can be destroyed by projectiles and rewards XP/score when destroyed.
 */
@Getter
@Setter
public class Destructible {
    private static int nextId = 1;

    private final int id;
    private final DestructibleType type;
    private float x;
    private float y;
    private final float width;
    private final float height;
    private int health;
    private final int maxHealth;
    private final int xpReward;
    private final int scoreReward;
    private long destroyedTime; // Time when destroyed, 0 if not destroyed

    public Destructible(float x, float y, float width, float height, int health,
                        int xpReward, int scoreReward, DestructibleType type) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.health = health;
        this.maxHealth = health;
        this.xpReward = xpReward;
        this.scoreReward = scoreReward;
        this.type = type;
        this.destroyedTime = 0;
    }

    /**
     * Applies damage to this destructible object.
     * @param damage amount of health to reduce
     * @return true if the destructible is destroyed (health <= 0), false otherwise
     */
    public boolean isTakeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            destroyedTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * Attempts to resurrect this destructible if it was destroyed.
     * @param respawnDelayMs time in milliseconds that must pass before respawning
     * @return true if successfully resurrected, false otherwise
     */
    public boolean isTryRespawn(long respawnDelayMs) {
        if (destroyedTime > 0 && System.currentTimeMillis() - destroyedTime >= respawnDelayMs) {
            health = maxHealth;
            destroyedTime = 0;
            return true;
        }
        return false;
    }

    /**
     * Returns the bounding rectangle for collision detection.
     */
    public Rect getBounds() {
        return new Rect(x, y, width, height);
    }

    @Override
    public String toString() {
        return String.format("Destructible(id=%d, type=%s, x=%.0f, y=%.0f, health=%d)",
                id, type.getName(), x, y, health);
    }
}
