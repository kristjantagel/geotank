package ee.taltech.wiseones.geotank.server.domain.model;

import ee.taltech.wiseones.geotank.server.domain.controller.Controller;
import ee.taltech.wiseones.geotank.server.domain.model.classupgrade.ClassUpgradeState;
import ee.taltech.wiseones.geotank.server.domain.model.experience.XpManager;
import ee.taltech.wiseones.geotank.server.domain.model.health.HealthManager;
import ee.taltech.wiseones.geotank.server.domain.model.skill.SkillManager;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;
import ee.taltech.wiseones.geotank.shared.message.dto.SkillType;
import lombok.Getter;
import lombok.Setter;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Represents a player in the game. Delegates specialized concerns to manager classes:
 * - XpManager: handles XP and leveling
 * - SkillManager: handles skill points and upgrades
 * - HealthManager: handles health, lives, death, and regen
 * - ClassUpgradeState: handles class selection and weight-based progression
 */
@Getter
@Setter
public class Player {
    private final int id;
    private String name;
    private int score;

    // Managers for specialized concerns
    private final XpManager xpManager = new XpManager();
    private final SkillManager skillManager = new SkillManager();
    private final HealthManager healthManager = new HealthManager();
    private final ClassUpgradeState classUpgrade = new ClassUpgradeState();

    private Tank lastHitByTank;
    private Tank tank;
    private Controller controller;

    public Player(int playerId) {
        this.id = playerId;
        healthManager.setLives(PLAYER_LIVES_COUNT);
    }

    // ===== XP & LEVELING =====

    public void addXp(int amount) {
        int[] newLevels = xpManager.addXp(amount);
        if (newLevels == null) {
            return;
        }
        for (int newLevel : newLevels) {
            skillManager.addSkillPoints(XpManager.getSkillPointsForLevel(newLevel));
        }
    }

    public int getLevel() {
        return xpManager.getLevel();
    }

    public int getXp() {
        return xpManager.getXp();
    }

    // ===== SKILLS =====

    public boolean isUpgradeSkill(SkillType type) {
        boolean result = skillManager.isUpgradeSkill(type);
        if (result) {
            applySkillEffects();
        }
        return result;
    }

    public int getSkillLevel(SkillType type) {
        return skillManager.getSkillLevel(type);
    }

    public int[] getSkillLevelsArray() {
        return skillManager.getSkillLevelsArray();
    }

    public int getSkillPoints() {
        return skillManager.getSkillPoints();
    }

    public void applySkillEffects() {
        skillManager.applyToTank(tank);
    }

    public int getEffectiveContactDamage() {
        if (!(tank instanceof SimpleTank simpleTank)) return 0;
        return (int) simpleTank.getBodyDamage();
    }

    /**
     * Get max hp
     */
    public int getMaxHp() {
        if (!(tank instanceof SimpleTank simpleTank)) return 0;
        return simpleTank.getMaxHp();
    }

    /**
     * Get total health regen
     */
    public float getHealthRegenPerSecond() {
        if (!(tank instanceof SimpleTank simpleTank)) return 0f;
        return simpleTank.getHpRegen();
    }

    // ===== HEALTH & LIVES =====

    public boolean isAlive() {
        return healthManager.isAlive();
    }

    public void setAlive(boolean alive) {
        healthManager.setAlive(alive);
    }

    public int getLives() {
        return healthManager.getLives();
    }

    public void setLives(int lives) {
        healthManager.setLives(lives);
    }

    public void takeDamage(int amount) {
        healthManager.takeDamage(amount);
    }

    /**
     * Reset all per-life state — XP, level, score, skill points and levels,
     * class upgrades, and the tank itself (a fresh SimpleTank with the same id
     * so the client's tracked tankId still matches; class-modified stats are
     * thrown away with the old instance). Caller is responsible for placing
     * the new tank at a valid spawn position.
     */
    public void respawn() {
        xpManager.reset();
        skillManager.reset();
        classUpgrade.reset();
        score = 0;
        lastHitByTank = null;
        if (tank != null) {
            tank = new SimpleTank(tank.getId());
        }
        healthManager.respawn(getMaxHp());
    }

    public boolean canTakeContactDamage(long currentTimeMs) {
        return healthManager.canTakeContactDamage(currentTimeMs);
    }

    public void recordContactDamage() {
        healthManager.recordContactDamage();
    }

    public void updateRegen(float deltaTime) {
        float regenRate = getHealthRegenPerSecond();
        healthManager.updateRegen(regenRate, getMaxHp(), deltaTime);
    }

    // ===== CLASS UPGRADES =====

    public boolean isNeedsClassSelection() {
        // Class selection needed when player has enough levels to earn another upgrade.
        // Upgrade tier is determined by: tier = playerLevel / CLASS_UPGRADE_INTERVAL_LEVELS
        // An upgrade is available when tier > upgradeLevel (i.e., tier has increased)
        int currentTier = getLevel() / CLASS_UPGRADE_INTERVAL_LEVELS;
        return currentTier > classUpgrade.getUpgradeLevel();
    }

    @Override
    public String toString() {
        return "Player{id=%d, name=%s, level=%d, class=%s}".formatted(
                id, name, getLevel(), classUpgrade.getPrimaryClass()
        );
    }
}
