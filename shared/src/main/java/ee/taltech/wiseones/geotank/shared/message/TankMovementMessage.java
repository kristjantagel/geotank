package ee.taltech.wiseones.geotank.shared.message;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TankMovementMessage {
    private int directionMask; // Bitmask for multiple simultaneous directions (supports diagonal movement)

    public TankMovementMessage(int directionMask) {
        this.directionMask = directionMask;
    }
}
