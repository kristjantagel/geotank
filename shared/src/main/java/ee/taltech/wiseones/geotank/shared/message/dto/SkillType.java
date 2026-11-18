package ee.taltech.wiseones.geotank.shared.message.dto;

/**
 * The 8 upgradeable stats (diep.io order, top to bottom in upgrade panel).
 * Ordinal matches the statIndex in SkillUpgradeMessage.
 */
public enum SkillType {
    HEALTH_REGEN,
    MAX_HEALTH,
    BODY_DAMAGE,
    BULLET_SPEED,
    PIERCE,
    BULLET_DAMAGE,
    FIRE_RATE,
    MOVEMENT_SPEED
}
