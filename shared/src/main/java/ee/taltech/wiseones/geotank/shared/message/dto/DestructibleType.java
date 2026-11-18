package ee.taltech.wiseones.geotank.shared.message.dto;

import lombok.Getter;

/**
 * Enum representing different types of destructible objects.
 */
@Getter
public enum DestructibleType {
    BOX(0, "Box"),
    SPHERE(1, "Sphere"),
    HEXAGON(2, "Hexagon");

    private final int id;
    private final String name;

    DestructibleType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static DestructibleType fromId(int id) {
        for (DestructibleType type : DestructibleType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return BOX; // default
    }
}
