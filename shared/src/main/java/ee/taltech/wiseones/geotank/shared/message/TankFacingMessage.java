package ee.taltech.wiseones.geotank.shared.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TankFacingMessage {
    private float angleRad;
}
