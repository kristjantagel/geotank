package ee.taltech.wiseones.geotank.shared.message;

import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class WelcomeMessage {
    private int playerId;
    private int tankId;

    private List<PlayerTankConfig> playerTankConfigs;
}
