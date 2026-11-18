package ee.taltech.wiseones.geotank.shared.message;

import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sent by the server to notify a player that they can upgrade their tank
 * Contains all available class options.
 */
@Getter
@NoArgsConstructor
public class ClassSelectionPromptMessage {
    private int playerId;
    private TankClass[] availableClasses;
    // Parallel array: non-null entry marks a multiclass card visual (primary + secondary).
    private TankClass[] availableSecondaryClasses;

    public ClassSelectionPromptMessage(int playerId, TankClass[] availableClasses,
                                       TankClass[] availableSecondaryClasses) {
        this.playerId = playerId;
        this.availableClasses = availableClasses;
        this.availableSecondaryClasses = availableSecondaryClasses;
    }
}
