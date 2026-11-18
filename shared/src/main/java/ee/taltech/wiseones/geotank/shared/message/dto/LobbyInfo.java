package ee.taltech.wiseones.geotank.shared.message.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LobbyInfo {
    private int lobbyId;
    private String lobbyName;
    private int playerCount;
    private int maxPlayers;
    private boolean inGame;

    public LobbyInfo() {
    }

    public LobbyInfo(int lobbyId, String lobbyName, int playerCount, int maxPlayers, boolean inGame) {
        this.lobbyId = lobbyId;
        this.lobbyName = lobbyName;
        this.playerCount = playerCount;
        this.maxPlayers = maxPlayers;
        this.inGame = inGame;
    }

}
