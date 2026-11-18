package ee.taltech.wiseones.geotank.shared.message.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LobbyPlayerInfo {
    private int playerId;
    private String playerName;
    private boolean ready;

    public LobbyPlayerInfo() {
    }

    public LobbyPlayerInfo(int playerId, String playerName, boolean ready) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.ready = ready;
    }

}
