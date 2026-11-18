package ee.taltech.wiseones.geotank.shared.message.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the state of a destructible object for network synchronization.
 */
@Data
@NoArgsConstructor
public class DestructibleState {
    private int id;
    private float x;
    private float y;
    private int health;
    private int type; // DestructibleType id

    public DestructibleState(int id, float x, float y, int health, int type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.health = health;
        this.type = type;
    }
}
