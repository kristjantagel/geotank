package ee.taltech.wiseones.geotank.clientcore.model;

import ee.taltech.wiseones.geotank.shared.message.dto.DestructibleState;
import ee.taltech.wiseones.geotank.shared.message.dto.DestructibleType;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a destructible object on the client side.
 */
@Getter
@Setter
public class DestructibleModel {
    private final int id;
    private float x;
    private float y;
    private int health;
    private final DestructibleType type;

    public DestructibleModel(int id, float x, float y, int health, DestructibleType type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.health = health;
        this.type = type;
    }

    public DestructibleModel(DestructibleState state) {
        this(state.getId(), state.getX(), state.getY(), state.getHealth(),
                DestructibleType.fromId(state.getType()));
    }

    @Override
    public String toString() {
        return String.format("DestructibleModel(id=%d, type=%s, x=%.0f, y=%.0f, health=%d)",
                id, type.getName(), x, y, health);
    }
}

