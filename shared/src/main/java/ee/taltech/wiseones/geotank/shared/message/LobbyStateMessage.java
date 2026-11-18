package ee.taltech.wiseones.geotank.shared.message;

import ee.taltech.wiseones.geotank.shared.message.dto.LobbyPlayerInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class LobbyStateMessage {
    private List<LobbyPlayerInfo> players = new ArrayList<>();
    private String lobbyName;
    private String lobbyCode; // null for public lobbies

    public LobbyStateMessage() {
    }

    public LobbyStateMessage(List<LobbyPlayerInfo> players) {
        this(players, null, null);
    }

    public LobbyStateMessage(List<LobbyPlayerInfo> players, String lobbyName, String lobbyCode) {
        this.players = new ArrayList<>(players);
        this.lobbyName = lobbyName;
        this.lobbyCode = lobbyCode;
    }
}
