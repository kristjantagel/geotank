package ee.taltech.wiseones.geotank.server.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Lightweight lobby player representation.
 * Only used during the lobby phase before the game starts.
 */
@Getter
public class LobbyPlayer {
    private final int connectionId;
    private final int playerId;
    private final String name;
    @Setter
    private boolean ready;

    public LobbyPlayer(int connectionId, int playerId, String name) {
        this.connectionId = connectionId;
        this.playerId = playerId;
        this.name = name;
        this.ready = false;
    }

    public void toggleReady() {
        this.ready = !this.ready;
    }
}
