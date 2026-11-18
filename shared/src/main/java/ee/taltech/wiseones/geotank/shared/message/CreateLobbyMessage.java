package ee.taltech.wiseones.geotank.shared.message;

import lombok.Getter;
import lombok.Setter;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_PLAYERS_PER_LOBBY;

@Setter
@Getter
public class CreateLobbyMessage {
    private String lobbyName;
    private String playerName;
    private int maxPlayers;
    private String code; // optional: null = public (shown in browser), non-null = private (code-only join)

    public CreateLobbyMessage() {
    }

    public CreateLobbyMessage(String lobbyName) {
        this.lobbyName = lobbyName;
        this.playerName = "Player";
        this.maxPlayers = MAX_PLAYERS_PER_LOBBY;
    }

    public CreateLobbyMessage(String lobbyName, String playerName) {
        this(lobbyName, playerName, MAX_PLAYERS_PER_LOBBY);
    }

    public CreateLobbyMessage(String lobbyName, String playerName, int maxPlayers) {
        this(lobbyName, playerName, maxPlayers, null);
    }

    public CreateLobbyMessage(String lobbyName, String playerName, int maxPlayers, String code) {
        this.lobbyName = lobbyName;
        this.playerName = playerName != null && !playerName.trim().isEmpty() ? playerName.trim() : "Player";
        this.maxPlayers = maxPlayers;
        this.code = code;
    }

}
