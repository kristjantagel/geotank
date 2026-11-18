package ee.taltech.wiseones.geotank.shared.message;

import lombok.Data;
import ee.taltech.wiseones.geotank.shared.message.dto.DestructibleState;
import ee.taltech.wiseones.geotank.shared.message.dto.ProjectileState;
import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankState;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GameStateMessage {
    private List<PlayerTankState> playerTankStates;
    private List<ProjectileState> projectileStates;
    private List<DestructibleState> destructibleStates;
}
