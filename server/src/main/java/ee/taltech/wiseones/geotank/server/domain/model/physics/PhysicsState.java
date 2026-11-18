package ee.taltech.wiseones.geotank.server.domain.model.physics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PhysicsState {
    private float x;
    private float y;

    private Vector2 speed;
    private Vector2 acceleration;
}
