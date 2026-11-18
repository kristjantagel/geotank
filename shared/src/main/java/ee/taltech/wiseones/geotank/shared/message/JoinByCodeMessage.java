package ee.taltech.wiseones.geotank.shared.message;

import lombok.Getter;
import lombok.Setter;

/**
 * Client→Server. Request to join a lobby by its host-chosen code.
 */
@Getter
@Setter
public class JoinByCodeMessage {
    private String code;
    private String playerName;

    public JoinByCodeMessage() {
    }

    public JoinByCodeMessage(String code, String playerName) {
        this.code = code;
        this.playerName = playerName != null && !playerName.trim().isEmpty() ? playerName.trim() : "Player";
    }
}
