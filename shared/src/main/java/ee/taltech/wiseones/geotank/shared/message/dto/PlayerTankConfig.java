package ee.taltech.wiseones.geotank.shared.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTankConfig {
    private int playerId;
    private int tankId;

    private float width;
    private float height;
}
