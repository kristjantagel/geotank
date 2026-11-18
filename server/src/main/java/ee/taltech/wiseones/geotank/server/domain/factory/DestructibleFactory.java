package ee.taltech.wiseones.geotank.server.domain.factory;

import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.shared.message.dto.DestructibleType;

import java.util.Random;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Factory for creating destructible objects with specific types and properties.
 */
public class DestructibleFactory {
    private DestructibleFactory() {
        /* This utility class should not be instantiated */
    }

    private static final Random random = new Random();

    /**
     * Creates a random destructible at the given coordinates.
     * Type is chosen based on spawn chances.
     */
    public static Destructible createRandomDestructible(float x, float y) {
        float rand = random.nextFloat();
        DestructibleType type;

        if (rand < BOX_SPAWN_CHANCE) {
            type = DestructibleType.BOX;
        } else if (rand < BOX_SPAWN_CHANCE + SPHERE_SPAWN_CHANCE) {
            type = DestructibleType.SPHERE;
        } else {
            type = DestructibleType.HEXAGON;
        }

        return createDestructible(x, y, type);
    }

    /**
     * Creates a destructible object of the specified type.
     */
    public static Destructible createDestructible(float x, float y, DestructibleType type) {
        return switch (type) {
            case BOX -> new Destructible(x, y, BOX_WIDTH, BOX_HEIGHT,
                    BOX_HEALTH, BOX_XP_REWARD, BOX_SCORE_REWARD, DestructibleType.BOX);
            case SPHERE -> new Destructible(x, y, SPHERE_WIDTH, SPHERE_HEIGHT,
                    SPHERE_HEALTH, SPHERE_XP_REWARD, SPHERE_SCORE_REWARD, DestructibleType.SPHERE);
            case HEXAGON -> new Destructible(x, y, HEXAGON_WIDTH, HEXAGON_HEIGHT,
                    HEXAGON_HEALTH, HEXAGON_XP_REWARD, HEXAGON_SCORE_REWARD, DestructibleType.HEXAGON);
        };
    }
}
