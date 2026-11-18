package ee.taltech.wiseones.geotank.shared.message;

import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sent by the client when the player selects a tank class.
 * Contains the player's selection during class upgrade event.
 * For multiclass selections, primaryClass is the main class and secondaryClass is the adjacent class.
 */
@Getter
@NoArgsConstructor
public class ClassSelectionMessage {
    private TankClass primaryClass;
    private TankClass secondaryClass;  // null for single-class selections

    public ClassSelectionMessage(TankClass primaryClass, TankClass secondaryClass) {
        this.primaryClass = primaryClass;
        this.secondaryClass = secondaryClass;
    }

    public ClassSelectionMessage(TankClass primaryClass) {
        this(primaryClass, null);
    }
}
