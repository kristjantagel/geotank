package ee.taltech.wiseones.geotank.shared.message;

import lombok.Getter;
import lombok.Setter;

/**
 * Client→Server. Request to join any available public lobby or auto-create one.
 */
@Getter
@Setter
public class QuickJoinMessage {
    private String playerName;

    public QuickJoinMessage() {
    }

    public QuickJoinMessage(String playerName) {
        this.playerName = playerName != null && !playerName.trim().isEmpty() ? playerName.trim() : "Player";
    }
}
