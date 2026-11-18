package ee.taltech.wiseones.geotank.shared.message;

import lombok.Getter;
import lombok.Setter;

public class JoinLobbyMessage {
    @Getter
    @Setter
    private int lobbyId;

    @Getter
    @Setter
    private String playerName;

    public JoinLobbyMessage() {
    }

    public JoinLobbyMessage(int lobbyId, String playerName) {
        this.lobbyId = lobbyId;
        this.playerName = playerName != null && !playerName.trim().isEmpty() ? playerName.trim() : "Player";
    }
}
