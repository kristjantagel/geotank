package ee.taltech.wiseones.geotank.server.application;

import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wraps a single game instance: its GameWorld, GameService, and the thread it runs on.
 * Each lobby that starts a game gets its own GameSession.
 */
@Getter
public class GameSession {
    private final int lobbyId;
    private final GameWorld gameWorld;
    @Setter
    private GameService gameService;
    private final AtomicReference<Thread> gameThread = new AtomicReference<>();
    private final Set<Integer> connectionIds = Collections.synchronizedSet(new HashSet<>());

    public GameSession(int lobbyId, GameWorld gameWorld) {
        this.lobbyId = lobbyId;
        this.gameWorld = gameWorld;
    }

    public void start() {
        if (gameService == null) {
            throw new IllegalStateException("GameService must be set before starting");
        }

        // Prepare the new thread
        Thread newThread = new Thread(gameService, "GameSession-Lobby-" + lobbyId);
        newThread.setDaemon(true);

        if (gameThread.compareAndSet(null, newThread)) {
            newThread.start();
        }
    }

    public void stop() {
        Thread existingThread = gameThread.getAndSet(null);

        if (existingThread != null) {
            existingThread.interrupt();
        }
    }

    public void removeConnection(int connectionId) {
        connectionIds.remove(connectionId);
        gameWorld.getPlayerManager().removePlayer(connectionId);
    }

    public boolean hasConnections() {
        return !connectionIds.isEmpty();
    }

}
