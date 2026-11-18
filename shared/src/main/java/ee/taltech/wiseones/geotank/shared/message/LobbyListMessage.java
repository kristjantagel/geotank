package ee.taltech.wiseones.geotank.shared.message;

import ee.taltech.wiseones.geotank.shared.message.dto.LobbyInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class LobbyListMessage {
    private List<LobbyInfo> lobbies = new ArrayList<>();

    public LobbyListMessage() {
    }

    public LobbyListMessage(List<LobbyInfo> lobbies) {
        this.lobbies = new ArrayList<>(lobbies);
    }

}
