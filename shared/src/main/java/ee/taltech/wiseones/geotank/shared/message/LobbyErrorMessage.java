package ee.taltech.wiseones.geotank.shared.message;

import lombok.Getter;
import lombok.Setter;

/**
 * Server→Client. Transient error surface for lobby operations
 * (e.g. code already taken, code not found, lobby full).
 */
@Getter
@Setter
public class LobbyErrorMessage {
    private String reason;

    public LobbyErrorMessage() {
    }

    public LobbyErrorMessage(String reason) {
        this.reason = reason;
    }
}
