package ee.taltech.wiseones.geotank.server.network.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import ee.taltech.wiseones.geotank.server.application.GameSession;
import ee.taltech.wiseones.geotank.server.domain.Lobby;
import ee.taltech.wiseones.geotank.server.domain.LobbyManager;
import ee.taltech.wiseones.geotank.server.domain.model.LobbyPlayer;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.network.ServerAdapter;

import java.util.Set;

import ee.taltech.wiseones.geotank.shared.message.CreateLobbyMessage;
import ee.taltech.wiseones.geotank.shared.message.GameLeaveMessage;
import ee.taltech.wiseones.geotank.shared.message.GameStartMessage;
import ee.taltech.wiseones.geotank.shared.message.JoinByCodeMessage;
import ee.taltech.wiseones.geotank.shared.message.JoinLobbyMessage;
import ee.taltech.wiseones.geotank.shared.message.LobbyErrorMessage;
import ee.taltech.wiseones.geotank.shared.message.LobbyListMessage;
import ee.taltech.wiseones.geotank.shared.message.LobbyListRequestMessage;
import ee.taltech.wiseones.geotank.shared.message.LobbyStateMessage;
import ee.taltech.wiseones.geotank.shared.message.PlayerReadyMessage;
import ee.taltech.wiseones.geotank.shared.message.QuickJoinMessage;
import ee.taltech.wiseones.geotank.shared.message.ChatMessage;
import ee.taltech.wiseones.geotank.shared.message.dto.LobbyPlayerInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_USERNAME_LENGTH;

// REFACTOR received() IN THE FUTURE

/**
 * Handles lobby-phase network messages from clients.
 * Routes create/join/leave/ready messages through the LobbyManager.
 */
public class LobbyListener extends Listener {
    public static final int MAX_LOBBY_NAME_LENGTH = 32;
    private final LobbyManager lobbyManager;
    private final ServerAdapter serverAdapter;

    public LobbyListener(LobbyManager lobbyManager, ServerAdapter serverAdapter) {
        this.lobbyManager = lobbyManager;
        this.serverAdapter = serverAdapter;
    }

    @Override
    public void received(Connection connection, Object object) {
        int connId = connection.getID();

        if (object instanceof CreateLobbyMessage msg) {
            String lobbyName = sanitizeLobbyName(msg.getLobbyName());
            String playerName = sanitizePlayerName(msg.getPlayerName());
            Lobby lobby = lobbyManager.createLobby(
                    lobbyName, connId, playerName, msg.getMaxPlayers(), msg.getCode());
            if (lobby != null) {
                sendLobbyState(lobby);
                Logger.info("Lobby created: " + lobbyName + " (maxPlayers=" + lobby.getMaxPlayers()
                        + ", private=" + lobby.isPrivate() + ")");
            } else {
                serverAdapter.sendToClient(connId, new LobbyErrorMessage("Code already in use"));
            }
        } else if (object instanceof QuickJoinMessage msg) {
            String playerName = sanitizePlayerName(msg.getPlayerName());
            Lobby lobby = lobbyManager.quickJoin(connId, playerName);
            if (lobbyManager.isConnectionInGame(connId)) {
                // QuickJoin routed us into an already-running match.
                serverAdapter.sendToClient(connId, new GameStartMessage());
            } else {
                sendLobbyState(lobby);
            }
        } else if (object instanceof JoinByCodeMessage msg) {
            String playerName = sanitizePlayerName(msg.getPlayerName());
            Lobby lobby = lobbyManager.getLobbyByCode(msg.getCode());
            if (lobby == null) {
                serverAdapter.sendToClient(connId, new LobbyErrorMessage("Lobby code not found or unavailable"));
            } else if (lobby.isGameStarted()) {
                Lobby jumped = lobbyManager.jumpIntoRunningGame(lobby.getLobbyId(), connId, playerName);
                if (jumped != null) {
                    serverAdapter.sendToClient(connId, new GameStartMessage());
                } else {
                    serverAdapter.sendToClient(connId, new LobbyErrorMessage("Game is full"));
                }
            } else {
                Lobby joined = lobbyManager.joinByCode(msg.getCode(), connId, playerName);
                if (joined != null) {
                    sendLobbyState(joined);
                } else {
                    serverAdapter.sendToClient(connId, new LobbyErrorMessage("Lobby unavailable"));
                }
            }
        } else if (object instanceof JoinLobbyMessage msg) {
            if (msg.getLobbyId() <= 0) {
                Logger.info("Invalid lobby ID: " + msg.getLobbyId());
                sendLobbyList(connId);
                return;
            }
            String playerName = sanitizePlayerName(msg.getPlayerName());
            Lobby target = lobbyManager.getLobbyById(msg.getLobbyId());
            if (target == null) {
                sendLobbyList(connId);
            } else if (target.isGameStarted()) {
                Lobby jumped = lobbyManager.jumpIntoRunningGame(msg.getLobbyId(), connId, playerName);
                if (jumped != null) {
                    serverAdapter.sendToClient(connId, new GameStartMessage());
                } else {
                    serverAdapter.sendToClient(connId, new LobbyErrorMessage("Game is full"));
                    sendLobbyList(connId);
                }
            } else {
                boolean success = lobbyManager.isJoinLobby(msg.getLobbyId(), connId, playerName);
                if (success) {
                    Lobby lobby = lobbyManager.getLobbyForConnection(connId);
                    if (lobby != null) sendLobbyState(lobby);
                } else {
                    sendLobbyList(connId);
                }
            }
        } else if (object instanceof LobbyListRequestMessage) {
            sendLobbyList(connId);
        } else if (object instanceof PlayerReadyMessage) {
            lobbyManager.toggleReady(connId);
            Lobby lobby = lobbyManager.getLobbyForConnection(connId);
            if (lobby != null) {
                sendLobbyState(lobby);
                // Check if game can start
                if (lobby.canStartGame()) {
                    GameSession session = lobbyManager.startGame(lobby, serverAdapter);
                    if (session != null) {
                        // Send GameStartMessage to all players
                        for (int cid : new HashSet<>(session.getConnectionIds())) {
                            serverAdapter.sendToClient(cid, new GameStartMessage());
                        }
                    }
                }
            }
        } else if (object instanceof ChatMessage msg) {
            // Re-build with server-authoritative sender name and sanitized message.
            GameSession session = lobbyManager.getGameSessionForConnection(connId);
            if (session != null) {
                Player sender = session.getGameWorld().getPlayerManager().getPlayerByConnectionId(connId);
                if (sender == null) return;
                broadcastChatToSession(session, new ChatMessage(sender.getName(), msg.getMessage()));
            } else {
                Lobby lobby = lobbyManager.getLobbyForConnection(connId);
                if (lobby == null) return;
                String authoritativeName = null;
                for (LobbyPlayer lp : lobby.getPlayers()) {
                    if (lp.getConnectionId() == connId) {
                        authoritativeName = lp.getName();
                        break;
                    }
                }
                if (authoritativeName == null) return;
                broadcastChatToLobby(lobby, new ChatMessage(authoritativeName, msg.getMessage()));
            }
        } else if (object instanceof GameLeaveMessage) {
            // Check game first — connectionToLobbyId is still populated while a game is running,
            // so we must consult the game session to avoid treating a mid-game leave as a lobby leave
            // and leaving the game thread orphaned.
            if (lobbyManager.isConnectionInGame(connId)) {
                lobbyManager.removeFromGame(connId);
            } else {
                Lobby lobby = lobbyManager.getLobbyForConnection(connId);
                if (lobby != null) {
                    int lobbyId = lobby.getLobbyId();
                    lobbyManager.leaveLobby(connId);
                    // Re-fetch lobby — leaveLobby may have removed it if now empty
                    Lobby remaining = lobbyManager.getLobbyById(lobbyId);
                    if (remaining != null && remaining.getPlayerCount() > 0) {
                        sendLobbyState(remaining);
                    }
                }
            }
            // Send lobby list to the leaving player (they're going back to browser)
            sendLobbyList(connId);
        }

        super.received(connection, object);
    }

    @Override
    public void disconnected(Connection connection) {
        Logger.info("Client disconnected: connection " + connection.getID());
        lobbyManager.handleDisconnect(connection.getID());
        super.disconnected(connection);
    }

    private void sendLobbyState(Lobby lobby) {
        List<LobbyPlayerInfo> playerInfos = new ArrayList<>();
        for (LobbyPlayer lp : lobby.getPlayers()) {
            playerInfos.add(new LobbyPlayerInfo(lp.getPlayerId(), lp.getName(), lp.isReady()));
        }
        LobbyStateMessage msg = new LobbyStateMessage(playerInfos, lobby.getLobbyName(), lobby.getCode());
        for (LobbyPlayer lp : lobby.getPlayers()) {
            serverAdapter.sendToClient(lp.getConnectionId(), msg);
        }
    }

    private void sendLobbyList(int connectionId) {
        serverAdapter.sendToClient(connectionId, new LobbyListMessage(lobbyManager.getLobbyList()));
    }

    private void broadcastChatToLobby(Lobby lobby, ChatMessage msg) {
        Logger.info("[CHAT] " + msg);
        for (LobbyPlayer lp : lobby.getPlayers()) {
            serverAdapter.sendToClient(lp.getConnectionId(), msg);
        }
    }

    private void broadcastChatToSession(GameSession session, ChatMessage msg) {
        Logger.info("[CHAT] " + msg);
        // Defensive copy — connectionIds can mutate mid-broadcast (late join / leave).
        Set<Integer> targets = new HashSet<>(session.getConnectionIds());
        for (int cid : targets) {
            serverAdapter.sendToClient(cid, msg);
        }
    }

    private String sanitizeLobbyName(String name) {
        if (name == null || name.isBlank()) {
            return "Lobby";
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_LOBBY_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_LOBBY_NAME_LENGTH);
        }
        return trimmed;
    }

    private String sanitizePlayerName(String name) {
        if (name == null || name.isBlank()) {
            return "Player";
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_USERNAME_LENGTH);
        }
        return trimmed;
    }
}
