package ee.taltech.wiseones.geotank.server.domain.model.physics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PhysicsProperties {
    private float width;
    private float height;

    private float maxSpeed;
}
