package ee.taltech.wiseones.geotank.shared.message.dto;

import lombok.Getter;

/**
 * Represents movement direction(s) for the tank.
 * Multiple directions can be combined as a bitmask in the movement message.
 */
@Getter
public enum Direction {
    NONE(0),
    UP(1),
    DOWN(2),
    LEFT(4),
    RIGHT(8);

    private final int bitmask;

    Direction(int bitmask) {
        this.bitmask = bitmask;
    }

    public static boolean hasUp(int mask) {
        return (mask & UP.bitmask) != 0;
    }

    public static boolean hasDown(int mask) {
        return (mask & DOWN.bitmask) != 0;
    }

    public static boolean hasLeft(int mask) {
        return (mask & LEFT.bitmask) != 0;
    }

    public static boolean hasRight(int mask) {
        return (mask & RIGHT.bitmask) != 0;
    }
}
