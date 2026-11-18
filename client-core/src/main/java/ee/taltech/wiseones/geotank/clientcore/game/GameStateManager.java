package ee.taltech.wiseones.geotank.clientcore.game;

import ee.taltech.wiseones.geotank.clientcore.audio.AudioManager;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.message.ChatMessage;
import ee.taltech.wiseones.geotank.shared.message.GameLeaveMessage;
import ee.taltech.wiseones.geotank.shared.message.GameStateMessage;
import ee.taltech.wiseones.geotank.shared.message.PlayerTankConfigMessage;
import ee.taltech.wiseones.geotank.shared.message.StatsUpdateMessage;
import ee.taltech.wiseones.geotank.shared.message.WelcomeMessage;
import ee.taltech.wiseones.geotank.shared.message.dto.LobbyInfo;
import ee.taltech.wiseones.geotank.shared.message.dto.LobbyPlayerInfo;
import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankConfig;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// TO MAKE THREAD-SAFE IN THE FUTURE

/**
 * Holds game state changes received from the network.
 * Thread-safe: Uses volatile for simple fields and ConcurrentHashMap for collections
 * to handle access from both the network thread (receiving messages) and render thread (reading state).
 */
@Getter
@Setter
public class GameStateManager {
    private final Map<Integer, PlayerTankConfig> playerTankConfigs = new ConcurrentHashMap<>();
    private final Set<Integer> leavedPlayers = Collections.synchronizedSet(new HashSet<>());

    // volatile for simple reads/writes that happen frequently from different threads
    private volatile GameStateMessage latestGameStateMessage;
    private volatile WelcomeMessage welcomeMessage;

    // Lobby state (from lobby system)
    private volatile List<LobbyPlayerInfo> lobbyPlayers = new ArrayList<>();
    private volatile String currentLobbyName = null;
    private volatile String currentLobbyCode = null;
    private volatile List<LobbyInfo> lobbyInfoList = new ArrayList<>();
    private volatile boolean gameStarting = false;
    private volatile String username = "Player";  // Username input on title screen
    private volatile String lobbyError = null;    // Latest transient lobby error (code taken, not found, ...)
    private volatile long lobbyErrorSetAt = 0;
    private final List<ChatMessage> chatMessages = Collections.synchronizedList(new ArrayList<>());

    // Stats (XP, level, score) — sent by server on changes
    private volatile StatsUpdateMessage latestStatsUpdate;
    private volatile boolean connected = true;
    private volatile boolean classSelectionAvailable = false; // Set when class upgrade prompt received
    private volatile TankClass[] availableClasses = null;     // Populated alongside classSelectionAvailable
    private volatile TankClass[] availableSecondaryClasses = null; // Parallel array: non-null entry = multiclass card
    private AudioManager audioManager;

    public void setDisconnected() {
        this.connected = false;
    }

    public void setPlayerTankConfigMessage(PlayerTankConfigMessage msg) {
        var conf = msg.getPlayerTankConfig();
        playerTankConfigs.put(conf.getPlayerId(), conf);
    }

    public boolean hasPlayerTankConfigs() {
        return !playerTankConfigs.isEmpty();
    }

    public Set<PlayerTankConfig> getPlayerTankConfigs() {
        return new HashSet<>(playerTankConfigs.values());
    }

    public void remove(PlayerTankConfig playerTankConfig) {
        this.playerTankConfigs.remove(playerTankConfig.getPlayerId());
    }

    public void setGameLeaveMessage(GameLeaveMessage gameLeaveMessage) {
        leavedPlayers.add(gameLeaveMessage.getPlayerId());
    }

    public boolean hasLeavedPLayers() {
        return !leavedPlayers.isEmpty();
    }

    public Set<Integer> getLeavedPlayers() {
        return new HashSet<>(leavedPlayers);
    }

    public void removeLeavedPlayer(int playerId) {
        this.leavedPlayers.remove(playerId);
    }

    public void clearLobbyState() {
        lobbyPlayers = new ArrayList<>();
        currentLobbyName = null;
        currentLobbyCode = null;
        lobbyInfoList = new ArrayList<>();
        gameStarting = false;
    }

    /**
     * Reset state that belongs to a single game session so stale data from
     * the previous session doesn't leak into the next one.
     */
    public void clearGameSessionState() {
        latestStatsUpdate = null;
        welcomeMessage = null;
        classSelectionAvailable = false;
        availableClasses = null;
        availableSecondaryClasses = null;
        playerTankConfigs.clear();
        leavedPlayers.clear();
        GameStateMessage empty = new GameStateMessage();
        empty.setPlayerTankStates(new ArrayList<>());
        empty.setProjectileStates(new ArrayList<>());
        latestGameStateMessage = empty;
    }


    public void setLobbyError(String reason) {
        this.lobbyError = reason;
        this.lobbyErrorSetAt = System.currentTimeMillis();
    }

    public void clearLobbyError() {
        this.lobbyError = null;
        this.lobbyErrorSetAt = 0;
    }

    /**
     * GameStateManager listens for GameStateMessages sent from the server
     * and forwards it to relevant components like the HUD and Arena.
     */
    public GameStateManager() {
        latestGameStateMessage = new GameStateMessage();
        latestGameStateMessage.setPlayerTankStates(new ArrayList<>());
        latestGameStateMessage.setProjectileStates(new ArrayList<>());

        welcomeMessage = null;
    }

    public void addChatMessage(ChatMessage msg) {
        chatMessages.add(msg);
    }

    public void clearChatMessages() {
        chatMessages.clear();
    }
}
