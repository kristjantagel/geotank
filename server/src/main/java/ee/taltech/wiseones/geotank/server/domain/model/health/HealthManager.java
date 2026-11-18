package ee.taltech.wiseones.geotank.server.domain.model.health;

import lombok.Getter;
import lombok.Setter;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Manages player health, lives, death state, and health regen.
 */
@Getter
@Setter
public class HealthManager {
    private int lives;
    private boolean alive = true;
    private long deathTimestamp;
    private float regenAccumulator;
    private long lastContactDamageAt;

    public HealthManager() {
        this.lives = PLAYER_LIVES_COUNT;
        this.regenAccumulator = 0;
        this.lastContactDamageAt = 0;
    }

    /**
     * Subtract damage from lives, clamped at 0. Alive flag is set by GameWorld.processKilledPlayer.
     */
    public void takeDamage(int amount) {
        lives -= amount;
        if (lives <= 0) {
            deathTimestamp = System.currentTimeMillis();
            lives = 0;
        }
    }

    /**
     * Heal the player, capped at max lives.
     */
    public void heal(int amount, int maxHealth) {
        lives = Math.min(lives + amount, maxHealth);
    }

    /**
     * Respawn the player.
     */
    public void respawn(int maxHealth) {
        alive = true;
        lives = maxHealth;
        deathTimestamp = 0;
        regenAccumulator = 0;
    }

    /**
     * Check if player can take contact damage.
     */
    public boolean canTakeContactDamage(long currentTimeMs) {
        return currentTimeMs - lastContactDamageAt >= TANK_CONTACT_DAMAGE_COOLDOWN_MS;
    }

    /**
     * Update last contact damage timestamp.
     */
    public void recordContactDamage() {
        lastContactDamageAt = System.currentTimeMillis();
    }

    /**
     * Process health regeneration.
     */
    public void updateRegen(float regenPerSecond, int maxHealth, float deltaTime) {
        if (!alive || lives <= 0 || regenPerSecond <= 0) return;

        regenAccumulator += regenPerSecond * deltaTime;
        int regenAmount = (int) regenAccumulator;

        if (regenAmount > 0) {
            heal(regenAmount, maxHealth);
            regenAccumulator -= regenAmount;
        }
    }
}
