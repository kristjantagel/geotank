package ee.taltech.wiseones.geotank.server.domain.model.terrain;

/**
 * @param frictionFactor   0 = free, 1 = strong braking
 * @param slipFactor       how much tank slips when turning or accelerating
 * @param recoilModifier   affects tank recoil when shooting
 * @param maxSpeedModifier modifies maximum speed on given surface
 */
public record TerrainProperties(float frictionFactor, float slipFactor, float recoilModifier, float maxSpeedModifier) {

    public static final float FRICTION_FACTOR = 0.1f;

    public static TerrainProperties defaultProperties() {
        return new TerrainProperties(
                FRICTION_FACTOR,  // frictionFactor: small braking, 0 = free, 1 = very strong
                0.0f,  // slipFactor: no slipping
                1.0f,  // recoilModifier: normal recoil, 1 = normal
                1.0f   // maxSpeedModifier: maximum speed normal
        );
    }
}
