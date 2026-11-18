package ee.taltech.wiseones.geotank.shared.message;

import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankConfig;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTankConfigMessage {
    PlayerTankConfig playerTankConfig;
}