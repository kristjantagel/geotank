package ee.taltech.wiseones.geotank.server.domain.model.skill;

import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;
import ee.taltech.wiseones.geotank.shared.message.dto.SkillType;
import lombok.Getter;

import java.util.Arrays;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Manages player skill points and skill upgrades.
 * Handles applying skill modifiers to the tank.
 */
@Getter
public class SkillManager {
    private int skillPoints;
    private final int[] skillLevels = new int[SkillType.values().length];

    public SkillManager() {
        this.skillPoints = 0;
    }

    public void reset() {
        skillPoints = 0;
        Arrays.fill(skillLevels, 0);
    }

    /**
     * Attempt to upgrade a skill stat. Returns true if successful.
     */
    public boolean isUpgradeSkill(SkillType type) {
        if (skillPoints <= 0) return false;
        int current = skillLevels[type.ordinal()];
        if (current >= MAX_SKILL_STAT_LEVEL) return false;
        skillLevels[type.ordinal()] = current + 1;
        skillPoints--;
        return true;
    }

    public int getSkillLevel(SkillType type) {
        return skillLevels[type.ordinal()];
    }

    public int[] getSkillLevelsArray() {
        return skillLevels.clone();
    }

    public void addSkillPoints(int amount) {
        this.skillPoints += amount;
    }

    /**
     * Apply additional stats from skills.
     *
     * @param tank simpleTank
     */
    public void applyToTank(Tank tank) {
        if (!(tank instanceof SimpleTank simpleTank)) return;

        // Get levels
        int regenLvl = getSkillLevel(SkillType.HEALTH_REGEN);
        int maxHpLvl = getSkillLevel(SkillType.MAX_HEALTH);
        int bodyDamageLvl = getSkillLevel(SkillType.BODY_DAMAGE);
        int bulletSpeedLvl = getSkillLevel(SkillType.BULLET_SPEED);
        int pierceLvl = getSkillLevel(SkillType.PIERCE);
        int bulletDamageLvl = getSkillLevel(SkillType.BULLET_DAMAGE);
        int fireRateLvl = getSkillLevel(SkillType.FIRE_RATE);
        int moveSpeedLvl = getSkillLevel(SkillType.MOVEMENT_SPEED);

        // Apply
        float regenBonus = regenLvl + SKILL_HEALTH_REGEN_BONUS;
        simpleTank.setHpRegen(simpleTank.getHpRegenPL() / 10 + regenBonus);

        float maxHpMult = 1f + maxHpLvl * SKILL_MAX_HEALTH_MULTIPLIER;
        simpleTank.setMaxHp((int) (simpleTank.getMaxHpPL() * maxHpMult));

        int bodyDamageBonus = (bodyDamageLvl * SKILL_BODY_DAMAGE_BONUS);
        simpleTank.setBodyDamage(simpleTank.getBodyDamagePL() + bodyDamageBonus);

        float bulletSpeedMult = 1f + bulletSpeedLvl * SKILL_BULLET_SPEED_MULTIPLIER;
        simpleTank.setBulletSpeed(simpleTank.getBulletSpeedPL() * bulletSpeedMult);

        simpleTank.setPierce(simpleTank.getPiercePL() + (pierceLvl * SKILL_BULLET_PIERCE_BONUS));

        simpleTank.setBulletDamage((int) (simpleTank.getBulletDamagePL() / 10
                + (bulletDamageLvl * SKILL_BULLET_DAMAGE_BONUS)));

        long reloadMs = (long) ((1000 / simpleTank.getFireRatePL()) * (1f - fireRateLvl * SKILL_FIRE_RATE_REDUCTION));
        simpleTank.setFireRateCooldownMs(reloadMs);

        float speed = simpleTank.getMoveSpeedPL() * (1f + moveSpeedLvl * SKILL_MOVEMENT_SPEED_MULTIPLIER);
        simpleTank.setMoveSpeed(speed);
        simpleTank.setAcceleration(speed * ACCELERATION_MULTIPLIER);
    }
}
