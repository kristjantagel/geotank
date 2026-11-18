package ee.taltech.wiseones.geotank.server.domain;

import ee.taltech.wiseones.geotank.shared.util.Logger;
import ee.taltech.wiseones.geotank.server.domain.model.LobbyPlayer;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-authoritative lobby state.
 * Thread-safe: all public methods are synchronized (KryoNet listener thread + LobbyService thread).
 */
public class Lobby {
    @Getter
    private final int lobbyId;
    @Getter
    private final String lobbyName;
    @Getter
    private final int maxPlayers;
    @Getter
    @Setter
    private volatile int targetTotalPlayers;
    @Getter
    private final String code; // null = public (appears in browser); non-null = private (code-only)
    @Getter
    private final boolean auto;
    private final Map<Integer, LobbyPlayer> playersByConnectionId = new HashMap<>();
    private int nextPlayerId = 1;
    private volatile boolean gameStarted;

    public Lobby(int lobbyId, String lobbyName, int maxPlayers) {
        this(lobbyId, lobbyName, maxPlayers, null, false);
    }

    public Lobby(int lobbyId, String lobbyName, int maxPlayers, String code, boolean auto) {
        this.lobbyId = lobbyId;
        this.lobbyName = lobbyName;
        this.maxPlayers = maxPlayers;
        this.targetTotalPlayers = maxPlayers;
        this.code = code;
        this.auto = auto;
    }

    public boolean isPrivate() {
        return code != null;
    }

    public synchronized boolean isFull() {
        return playersByConnectionId.size() >= maxPlayers;
    }

    public synchronized int getPlayerCount() {
        return playersByConnectionId.size();
    }

    public synchronized void addPlayer(int connectionId, String name) {
        int playerId = nextPlayerId++;
        LobbyPlayer player = new LobbyPlayer(connectionId, playerId, name);
        playersByConnectionId.put(connectionId, player);

        Logger.info("Player joined: " + name + " (id=" + playerId + ", conn=" + connectionId + ")");
    }

    public synchronized void removePlayer(int connectionId) {
        LobbyPlayer removed = playersByConnectionId.remove(connectionId);
        if (removed != null) {

            Logger.info("Player left: " + removed.getName() + " (id=" + removed.getPlayerId() + ")");
        }
    }

    public synchronized void toggleReady(int connectionId) {
        if (gameStarted) return;
        LobbyPlayer player = playersByConnectionId.get(connectionId);
        if (player != null) {
            player.toggleReady();

            Logger.info("Player " + player.getName() + " ready=" + player.isReady());
        }
    }

    public synchronized boolean canStartGame() {
        if (playersByConnectionId.isEmpty()) {
            return false;
        }
        for (LobbyPlayer player : playersByConnectionId.values()) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }

    public synchronized Collection<LobbyPlayer> getPlayers() {
        return Collections.unmodifiableCollection(playersByConnectionId.values());
    }

    public synchronized boolean isGameStarted() {
        return gameStarted;
    }

    public synchronized void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }
}
