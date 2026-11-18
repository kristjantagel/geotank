package ee.taltech.wiseones.geotank.server.domain.controller.strategy.util;

/**
 * Shared utility methods for movement calculations
 */
public class MovementUtils {
    private MovementUtils() {
        /* This utility class should not be instantiated */
    }

    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;

    /**
     * Convert an angle to a direction mask.
     *
     * @param angle the angle in radians
     * @return direction mask
     */
    public static int angleToDirectionMask(float angle) {
        // Normalize angle to 0-2 pi
        angle = (float) (angle % (2 * Math.PI));
        if (angle < 0) angle += (float) (2 * Math.PI);

        if (angle < Math.PI / 8) {
            return RIGHT;  // 0° to 22.5°
        } else if (angle < 3 * Math.PI / 8) {
            return UP | RIGHT;  // 22.5° to 67.5°
        } else if (angle < 5 * Math.PI / 8) {
            return UP;     // 67.5° to 112.5°
        } else if (angle < 7 * Math.PI / 8) {
            return UP | LEFT;  // 112.5° to 157.5°
        } else if (angle < 9 * Math.PI / 8) {
            return LEFT;   // 157.5° to 202.5°
        } else if (angle < 11 * Math.PI / 8) {
            return DOWN | LEFT;  // 202.5° to 247.5°
        } else if (angle < 13 * Math.PI / 8) {
            return DOWN;   // 247.5° to 292.5°
        } else if (angle < 15 * Math.PI / 8) {
            return DOWN | RIGHT;  // 292.5° to 337.5°
        } else {
            return RIGHT;  // 337.5° to 360°
        }
    }

    public static int calculateDirectionMask(float fromX, float fromY, float toX, float toY) {
        float angle = angle(fromX, fromY, toX, toY);
        return angleToDirectionMask(angle);
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static float angle(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }
}
