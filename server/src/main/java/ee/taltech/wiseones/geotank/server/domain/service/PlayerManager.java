package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.model.ConnectedPlayer;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import ee.taltech.wiseones.geotank.shared.constant.Constants;
import lombok.Getter;

import java.util.*;

/**
 * Manages player lifecycle and state tracking
 */
public class PlayerManager {
    private final Map<Integer, ConnectedPlayer> playersByConnectionId = new HashMap<>();
    private final Map<Integer, Player> playersById = new HashMap<>();
    private final Set<Integer> newConnectionIds = new HashSet<>();
    private final Set<Integer> newPlayerIds = new HashSet<>();
    private final Set<Integer> leavedPlayerIds = new HashSet<>();
    @Getter
    private final Set<Integer> playersWithUpdates = new HashSet<>();

    private int nextPlayerId = 1;

    public synchronized Player createPlayer() {
        int playerId = nextPlayerId++;
        Logger.info("Creating player with ID: " + playerId);
        Player player = new Player(playerId);
        player.setLives(Constants.BASE_HEALTH);
        return player;
    }

    public synchronized void addPlayer(Player player) {
        playersById.put(player.getId(), player);
        newPlayerIds.add(player.getId());
    }

    public synchronized void addPlayer(int connectionId, Player player) {
        playersByConnectionId.put(connectionId, new ConnectedPlayer(connectionId, player));
        newConnectionIds.add(connectionId);
    }

    public synchronized void removePlayer(int connectionId) {
        newConnectionIds.remove(connectionId);
        ConnectedPlayer connectedPlayer = playersByConnectionId.remove(connectionId);
        if (connectedPlayer != null) {
            removePlayer(connectedPlayer.player());
        }
    }

    public synchronized void removePlayer(Player player) {
        leavedPlayerIds.add(player.getId());
        playersById.remove(player.getId());
    }

    public synchronized Collection<Player> getAllPlayers() {
        return new ArrayList<>(playersById.values()); // defensive copy for thread safety
    }

    public synchronized Player getPlayerByConnectionId(int connectionId) {
        return Optional.ofNullable(playersByConnectionId.get(connectionId))
                .map(ConnectedPlayer::player)
                .orElse(null);
    }

    public synchronized Player getPlayerById(int playerId) {
        return playersById.get(playerId);
    }

    public synchronized Player getPlayerByTank(Object tank) {
        for (Player player : playersById.values()) {
            if (player.getTank() == tank) {
                return player;
            }
        }
        return null;
    }

    public synchronized int getConnectionIdByPlayerId(int playerId) {
        for (Map.Entry<Integer, ConnectedPlayer> entry : playersByConnectionId.entrySet()) {
            if (entry.getValue().player().getId() == playerId) {
                return entry.getKey();
            }
        }
        return -1; // Player not found or not connected
    }

    public synchronized Set<Integer> getNewConnectionIds() {
        return new HashSet<>(newConnectionIds); // defensive copy
    }

    public synchronized void notNewConnection(int connectionId) {
        newConnectionIds.remove(connectionId);
    }

    public synchronized Set<Integer> getNewPlayerIds() {
        return new HashSet<>(newPlayerIds); // defensive copy
    }

    public synchronized void notNewPlayer(int playerId) {
        newPlayerIds.remove(playerId);
    }

    public synchronized Set<Integer> getLeavedPlayerIds() {
        return new HashSet<>(leavedPlayerIds);
    }

    public synchronized void notLeavedPlayer(int playerId) {
        leavedPlayerIds.remove(playerId);
    }
}
