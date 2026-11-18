package ee.taltech.wiseones.geotank.server.domain;

import ee.taltech.wiseones.geotank.server.application.GameService;
import ee.taltech.wiseones.geotank.server.application.GameSession;
import ee.taltech.wiseones.geotank.server.domain.factory.PlayerFactory;
import ee.taltech.wiseones.geotank.server.domain.factory.TankFactory;
import ee.taltech.wiseones.geotank.server.domain.model.LobbyPlayer;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.physics.Rect;
import ee.taltech.wiseones.geotank.server.domain.service.*;
import ee.taltech.wiseones.geotank.server.network.ScopedNetworkSender;
import ee.taltech.wiseones.geotank.server.network.ServerAdapter;
import ee.taltech.wiseones.geotank.shared.message.LobbyStateMessage;
import ee.taltech.wiseones.geotank.shared.message.dto.LobbyInfo;
import ee.taltech.wiseones.geotank.shared.message.dto.LobbyPlayerInfo;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

// TO REFACTOR IN THE FUTURE

/**
 * Central coordinator for all lobbies and game sessions.
 * Thread-safe: all public methods are synchronized.
 * Lock ordering: LobbyManager lock is always acquired before Lobby lock.
 */
public class LobbyManager {
    public static final int MAX_CODE_LENGTH = 12;
    private final Map<Integer, Lobby> lobbiesById = new HashMap<>();
    private final Map<Integer, GameSession> gameSessionsByLobbyId = new HashMap<>();
    private final Map<Integer, Integer> connectionToLobbyId = new HashMap<>();
    private final Map<Integer, GameSession> connectionToGameSession = new HashMap<>();
    private final Map<String, Integer> codeToLobbyId = new HashMap<>();
    @Setter
    private ServerAdapter serverAdapter;
    private int nextLobbyId = 1;
    private int nextAutoLobbyNumber = 1;
    private final java.util.Random random = new java.util.Random();

    /**
     * Create a lobby with an optional host-chosen code.
     * If code is non-null and already taken, returns null (caller should send LobbyErrorMessage).
     * Code is normalized to upper case and trimmed; blank → treated as null.
     */
    public synchronized Lobby createLobby(String name, int connectionId, String playerName,
                                          int requestedMaxPlayers, String requestedCode) {
        String normalizedCode = normalizeCode(requestedCode);
        if (normalizedCode != null && codeToLobbyId.containsKey(normalizedCode)) {
            Logger.info("Lobby create rejected: code '" + normalizedCode + "' already in use");
            return null;
        }
        int clampedMaxPlayers = clampMaxPlayers(requestedMaxPlayers);
        int lobbyId = nextLobbyId++;
        Lobby lobby = new Lobby(lobbyId, name, clampedMaxPlayers, normalizedCode, false);
        lobbiesById.put(lobbyId, lobby);
        if (normalizedCode != null) {
            codeToLobbyId.put(normalizedCode, lobbyId);
        }
        lobby.addPlayer(connectionId, playerName);
        connectionToLobbyId.put(connectionId, lobbyId);
        Logger.info("Lobby created: " + name + " (id=" + lobbyId + ", maxPlayers=" + clampedMaxPlayers
                + ", code=" + normalizedCode + ") by connection " + connectionId);
        return lobby;
    }

    /**
     * Route the connection into a match with the least friction:
     * 1. A public waiting-room lobby with space (prefer auto lobbies);
     * 2. A public running game with a free human slot (jump in);
     * 3. A brand-new auto lobby.
     * Caller distinguishes cases 1+3 (lobby waiting room — send LobbyStateMessage)
     * from case 2 (game already running — send GameStartMessage) via
     * {@link #isConnectionInGame(int)}.
     */
    public synchronized Lobby quickJoin(int connectionId, String playerName) {
        Lobby existing = pickQuickJoinCandidate();
        if (existing != null) {
            existing.addPlayer(connectionId, playerName);
            connectionToLobbyId.put(connectionId, existing.getLobbyId());
            Logger.info("QuickJoin: connection " + connectionId + " joined auto/public lobby "
                    + existing.getLobbyId());
            return existing;
        }
        Lobby running = pickRunningGameCandidate();
        if (running != null) {
            attachConnectionToGame(running, connectionId, playerName);
            Logger.info("QuickJoin: connection " + connectionId + " jumped into running game "
                    + running.getLobbyId());
            return running;
        }
        // No suitable lobby — create a new auto lobby
        int lobbyId = nextLobbyId++;
        String autoName = "Quick Match #" + nextAutoLobbyNumber++;
        Lobby lobby = new Lobby(lobbyId, autoName, MAX_PLAYERS_AUTO_LOBBY, null, true);
        lobbiesById.put(lobbyId, lobby);
        lobby.addPlayer(connectionId, playerName);
        connectionToLobbyId.put(connectionId, lobbyId);
        Logger.info("QuickJoin: created auto lobby " + autoName + " (id=" + lobbyId + ")");
        return lobby;
    }

    private Lobby pickQuickJoinCandidate() {
        List<Lobby> autoCandidates = new ArrayList<>();
        List<Lobby> publicCandidates = new ArrayList<>();
        for (Lobby lobby : lobbiesById.values()) {
            if (lobby.isPrivate() || lobby.isFull() || lobby.isGameStarted()) continue;
            if (lobby.isAuto()) autoCandidates.add(lobby);
            else publicCandidates.add(lobby);
        }
        if (!autoCandidates.isEmpty()) {
            return autoCandidates.get(random.nextInt(autoCandidates.size()));
        }
        if (!publicCandidates.isEmpty()) {
            return publicCandidates.get(random.nextInt(publicCandidates.size()));
        }
        return null;
    }

    private Lobby pickRunningGameCandidate() {
        List<Lobby> candidates = new ArrayList<>();

        for (Lobby lobby : lobbiesById.values()) {
            GameSession session = gameSessionsByLobbyId.get(lobby.getLobbyId());

            if (!lobby.isPrivate() && lobby.isGameStarted() && session != null &&
                    session.getConnectionIds().size() < lobby.getMaxPlayers()) {

                candidates.add(lobby);
            }
        }

        if (candidates.isEmpty()) return null;

        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Look up a lobby by host-chosen code (case-insensitive). Returns null if not found,
     * full, already in game, or the connection can't join.
     */
    public synchronized Lobby joinByCode(String code, int connectionId, String playerName) {
        String normalized = normalizeCode(code);
        if (normalized == null) {
            Logger.info("JoinByCode rejected: empty code");
            return null;
        }
        Integer lobbyId = codeToLobbyId.get(normalized);
        if (lobbyId == null) {
            Logger.info("JoinByCode rejected: no lobby with code '" + normalized + "'");
            return null;
        }
        Lobby lobby = lobbiesById.get(lobbyId);
        if (lobby == null || lobby.isFull() || lobby.isGameStarted()) {
            Logger.info("JoinByCode rejected: lobby " + lobbyId + " unavailable");
            return null;
        }
        lobby.addPlayer(connectionId, playerName);
        connectionToLobbyId.put(connectionId, lobbyId);
        Logger.info("JoinByCode: connection " + connectionId + " joined lobby " + lobbyId + " via code");
        return lobby;
    }

    private String normalizeCode(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > MAX_CODE_LENGTH) trimmed = trimmed.substring(0, MAX_CODE_LENGTH);
        return trimmed.toUpperCase();
    }

    private int clampMaxPlayers(int requested) {
        if (requested < MIN_PLAYERS_CUSTOM_LOBBY) return MIN_PLAYERS_CUSTOM_LOBBY;
        return Math.min(requested, MAX_PLAYERS_CUSTOM_LOBBY);
    }

    private void releaseLobbyResources(Lobby lobby) {
        if (lobby == null) return;
        if (lobby.getCode() != null) {
            codeToLobbyId.remove(lobby.getCode());
        }
    }

    public synchronized boolean isJoinLobby(int lobbyId, int connectionId, String playerName) {
        Lobby lobby = lobbiesById.get(lobbyId);
        String reason = "";
        if (lobby == null) {
            reason = lobbyId + " does not exist";
            Logger.error("Join failed: lobby " + reason);
            return false;
        } else if (lobby.isPrivate()) {
            reason = lobbyId + " is private (code required)";
        } else if (lobby.isFull()) {
            reason = lobbyId + " is full";
        } else if (lobby.isGameStarted()) {
            reason = lobbyId + " is already in game";
        }
        if (!reason.isEmpty()) {
            Logger.error("Join failed: lobby " + reason);
            return false;
        }
        lobby.addPlayer(connectionId, playerName);
        connectionToLobbyId.put(connectionId, lobbyId);
        return true;
    }

    public synchronized void leaveLobby(int connectionId) {
        Integer lobbyId = connectionToLobbyId.remove(connectionId);
        if (lobbyId == null) return;

        Lobby lobby = lobbiesById.get(lobbyId);
        if (lobby == null) return;

        lobby.removePlayer(connectionId);
        if (lobby.getPlayerCount() == 0) {
            lobbiesById.remove(lobbyId);
            releaseLobbyResources(lobby);
            Logger.info("Lobby " + lobbyId + " removed (empty)");
        }
    }

    public synchronized void toggleReady(int connectionId) {
        Integer lobbyId = connectionToLobbyId.get(connectionId);
        if (lobbyId == null) return;

        Lobby lobby = lobbiesById.get(lobbyId);
        if (lobby != null) {
            lobby.toggleReady(connectionId);
        }
    }

    public synchronized Lobby getLobbyById(int lobbyId) {
        return lobbiesById.get(lobbyId);
    }

    public synchronized Lobby getLobbyForConnection(int connectionId) {
        Integer lobbyId = connectionToLobbyId.get(connectionId);
        if (lobbyId == null) return null;
        return lobbiesById.get(lobbyId);
    }

    public synchronized GameWorld getGameWorldForConnection(int connectionId) {
        GameSession session = connectionToGameSession.get(connectionId);
        if (session == null) return null;
        return session.getGameWorld();
    }

    public synchronized boolean isConnectionInGame(int connectionId) {
        return connectionToGameSession.containsKey(connectionId);
    }

    public synchronized GameSession getGameSessionForConnection(int connectionId) {
        return connectionToGameSession.get(connectionId);
    }

    public synchronized Lobby getLobbyByCode(String code) {
        String normalized = normalizeCode(code);
        if (normalized == null) return null;
        Integer lobbyId = codeToLobbyId.get(normalized);
        if (lobbyId == null) return null;
        return lobbiesById.get(lobbyId);
    }

    /**
     * Attach a connection to an already-running game session (late join).
     * Returns the lobby on success, null if the lobby isn't running, the session is gone,
     * or the game has no free human slots.
     * Late joiners start fresh (no XP carryover) — PlayerFactory.createConnectedPlayer
     * returns a baseline player. AISpawner will remove one AI on its next tick to make
     * room (targetAI = targetTotal - humanCount).
     */
    public synchronized Lobby jumpIntoRunningGame(int lobbyId, int connectionId, String playerName) {
        Lobby lobby = lobbiesById.get(lobbyId);
        String rejectionPrefix = "JumpIn rejected: lobby ";
        if (lobby == null) {
            Logger.info(rejectionPrefix + lobbyId + " does not exist");
            return null;
        }
        if (lobby.isPrivate()) {
            Logger.info(rejectionPrefix + lobbyId + " is private (code required)");
            return null;
        }
        if (!lobby.isGameStarted()) {
            Logger.info(rejectionPrefix + lobbyId + " is still in waiting room");
            return null;
        }
        GameSession session = gameSessionsByLobbyId.get(lobbyId);
        if (session == null) {
            Logger.info(rejectionPrefix + lobbyId + " has no active game session");
            return null;
        }
        if (session.getConnectionIds().size() >= lobby.getMaxPlayers()) {
            Logger.info(rejectionPrefix + lobbyId + " is at human cap");
            return null;
        }
        attachConnectionToGame(lobby, connectionId, playerName);
        Logger.info("JumpIn: connection " + connectionId + " jumped into lobby " + lobbyId);
        return lobby;
    }

    /**
     * Shared helper: spin up a connected player in the running game session for `lobby`.
     * Caller must already hold the LobbyManager monitor and have validated capacity.
     * Spawn is assigned BEFORE the player is added to PlayerManager so the game tick
     * can't include the new tank at an uninitialized (0,0) position on an interleaved tick.
     */
    private void attachConnectionToGame(Lobby lobby, int connectionId, String playerName) {
        GameSession session = gameSessionsByLobbyId.get(lobby.getLobbyId());
        GameWorld gameWorld = session.getGameWorld();
        PlayerManager playerManager = gameWorld.getPlayerManager();

        PlayerFactory playerFactory = new PlayerFactory(gameWorld, new TankFactory());
        Player player = playerFactory.createConnectedPlayer(playerName);

        new SpawnService().spawnPlayerRandomly(
                player.getTank(), playerManager.getAllPlayers(), gameWorld.getAllDestructibles());

        playerManager.addPlayer(player);
        playerManager.addPlayer(connectionId, player);

        session.getConnectionIds().add(connectionId);
        connectionToGameSession.put(connectionId, session);
        connectionToLobbyId.put(connectionId, lobby.getLobbyId());
    }

    public synchronized List<LobbyInfo> getLobbyList() {
        List<LobbyInfo> list = new ArrayList<>();
        for (Lobby lobby : lobbiesById.values()) {
            if (lobby.isPrivate()) continue; // code-only lobbies are hidden from the browser
            int playerCount;
            if (lobby.isGameStarted()) {
                GameSession session = gameSessionsByLobbyId.get(lobby.getLobbyId());
                playerCount = (session != null) ? session.getConnectionIds().size() : lobby.getPlayerCount();
            } else {
                playerCount = lobby.getPlayerCount();
            }
            list.add(new LobbyInfo(
                    lobby.getLobbyId(),
                    lobby.getLobbyName(),
                    playerCount,
                    lobby.getMaxPlayers(),
                    lobby.isGameStarted()));
        }
        return list;
    }

    public synchronized GameSession startGame(Lobby lobby, ServerAdapter serverAdapter) {
        if (lobby.isGameStarted()) {
            Logger.info("Game start aborted for lobby " + lobby.getLobbyId() + " — already started");
            return null;
        }
        // Re-verify start conditions (player may have disconnected between check and call)
        if (!lobby.canStartGame()) {
            Logger.info("Game start aborted for lobby " + lobby.getLobbyId()
                    + " — start conditions no longer met");
            return null;
        }
        lobby.setGameStarted(true);

        try {
            GameWorld gameWorld = createGameWorld();
            TankFactory tankFactory = new TankFactory();
            PlayerFactory playerFactory = new PlayerFactory(gameWorld, tankFactory);
            PlayerManager playerManager = gameWorld.getPlayerManager();

            // Snapshot player list to avoid inconsistency between two iterations
            List<LobbyPlayer> players = new ArrayList<>(lobby.getPlayers());

            // Transfer lobby players into the game world
            for (LobbyPlayer lobbyPlayer : players) {
                Player player = playerFactory.createConnectedPlayer(
                        lobbyPlayer.getName());
                Logger.info("Creating Player " + player.getId()
                        + " for lobby player " + lobbyPlayer.getName());

                playerManager.addPlayer(player);
                playerManager.addPlayer(lobbyPlayer.getConnectionId(), player);
            }

            // Pad the game with AI players up to (target - humans) AND the human-count cap.
            // Same formula AISpawner uses each reconcile tick — keeping them in sync avoids
            // an over-spawn flash at game start that the reconciler would trim on tick 1.
            int targetTotal = lobby.getTargetTotalPlayers();
            int humanCount = playerManager.getAllPlayers().size();
            int aiCap = AISpawner.aiCapForHumanCount(humanCount);
            int aiPlayersNeeded = Math.clamp((long) targetTotal - humanCount, 0, aiCap);
            for (int i = 0; i < aiPlayersNeeded; i++) {
                Player aiPlayer = playerFactory.createAIPlayer();
                Logger.info("Creating AI Player " + aiPlayer.getId());
                playerManager.addPlayer(aiPlayer);
            }

            // Initialize destructible objects
            gameWorld.initializeDestructibles();

            // Spawn all players at random positions
            SpawnService spawnService = new SpawnService();
            for (Player player : playerManager.getAllPlayers()) {
                spawnService.spawnPlayerRandomly(
                        player.getTank(), playerManager.getAllPlayers(), gameWorld.getAllDestructibles());
            }

            // Create scoped sender for this game session
            GameSession session = new GameSession(lobby.getLobbyId(), gameWorld);
            for (LobbyPlayer lp : players) {
                session.getConnectionIds().add(lp.getConnectionId());
                connectionToGameSession.put(lp.getConnectionId(), session);
            }

            ScopedNetworkSender scopedSender = new ScopedNetworkSender(serverAdapter, session.getConnectionIds());
            AISpawner aiSpawner = new AISpawner(gameWorld, playerFactory);
            Runnable aiReconciler = () -> aiSpawner.reconcile(
                    lobby.getTargetTotalPlayers(),
                    session.getConnectionIds().size());
            GameService gameService = new GameService(gameWorld, scopedSender, aiReconciler);
            session.setGameService(gameService);
            session.start();

            gameSessionsByLobbyId.put(lobby.getLobbyId(), session);

            Logger.info("Game started for lobby " + lobby.getLobbyId()
                    + " with " + players.size() + " players");
            return session;
        } catch (Exception e) {
            // Reset lobby state so players aren't stuck
            lobby.setGameStarted(false);
            Logger.error("Failed to start game for lobby " + lobby.getLobbyId() + ": " + e.getMessage());
            return null;
        }
    }

    public synchronized void removeFromGame(int connectionId) {
        GameSession session = connectionToGameSession.remove(connectionId);
        if (session == null) return;

        // Also clear lobby mapping to prevent orphaned entries
        connectionToLobbyId.remove(connectionId);
        session.removeConnection(connectionId);

        if (!session.hasConnections()) {
            session.stop();
            gameSessionsByLobbyId.remove(session.getLobbyId());
            Lobby removed = lobbiesById.remove(session.getLobbyId());
            releaseLobbyResources(removed);
            Logger.info("Game session for lobby " + session.getLobbyId() + " stopped (no players)");
        }
    }

    public synchronized void handleDisconnect(int connectionId) {
        // Check if in a game session
        if (connectionToGameSession.containsKey(connectionId)) {
            removeFromGame(connectionId);
        }
        // Check if in a lobby
        Integer lobbyId = connectionToLobbyId.get(connectionId);
        if (lobbyId != null) {
            leaveLobby(connectionId);
            // Notify remaining lobby players of the updated state
            Lobby lobby = lobbiesById.get(lobbyId);
            if (lobby != null && lobby.getPlayerCount() > 0 && serverAdapter != null) {
                broadcastLobbyState(lobby);
            }
        }
    }

    private void broadcastLobbyState(Lobby lobby) {
        List<LobbyPlayerInfo> playerInfos = new ArrayList<>();
        for (LobbyPlayer lp : lobby.getPlayers()) {
            playerInfos.add(new LobbyPlayerInfo(lp.getPlayerId(), lp.getName(), lp.isReady()));
        }
        LobbyStateMessage msg = new LobbyStateMessage(playerInfos, lobby.getLobbyName(), lobby.getCode());
        for (LobbyPlayer lp : lobby.getPlayers()) {
            try {
                serverAdapter.sendToClient(lp.getConnectionId(), msg);
            } catch (Exception e) {
                Logger.debug("Failed to notify connection " + lp.getConnectionId());
            }
        }
    }

    private GameWorld createGameWorld() {
        TankMovementService tankMovementService = new TankMovementService();
        CollisionService collisionService = new CollisionService(
                new Rect(ARENA_LOWER_BOUND_X, ARENA_LOWER_BOUND_Y, ARENA_UPPER_BOUND_X, ARENA_UPPER_BOUND_Y));
        ProjectileMover projectileMover = new ProjectileMover();
        ShootingService shootingService = new ShootingService();
        return new GameWorld(collisionService, tankMovementService, shootingService, projectileMover);
    }
}
