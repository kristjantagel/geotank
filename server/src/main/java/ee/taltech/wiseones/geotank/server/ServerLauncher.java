package ee.taltech.wiseones.geotank.server;

import com.esotericsoftware.kryonet.Server;
import ee.taltech.wiseones.geotank.server.domain.LobbyManager;
import ee.taltech.wiseones.geotank.server.network.ServerAdapter;
import ee.taltech.wiseones.geotank.server.network.listener.ClassSelectionListener;
import ee.taltech.wiseones.geotank.server.network.listener.LobbyListener;
import ee.taltech.wiseones.geotank.server.network.listener.RespawnListener;
import ee.taltech.wiseones.geotank.server.network.listener.SkillUpgradeListener;
import ee.taltech.wiseones.geotank.server.network.listener.TankMovementListener;
import ee.taltech.wiseones.geotank.server.network.listener.TankShootingListener;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.io.IOException;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Game server launcher.
 * Starts the server, registers listeners, and runs continuously.
 * Multiple lobbies and games can run simultaneously.
 */
public class ServerLauncher {

    public static void main(String[] args) {
        try {
            LobbyManager lobbyManager = new LobbyManager();
            // Increase buffer sizes for UDP to handle large GameStateMessage with many projectiles/destructibles
            // Default was too small.
            Server server = new Server(WRITE_BUFFER_SIZE, WRITE_BUFFER_SIZE);
            ServerAdapter serverAdapter = new ServerAdapter(server);
            lobbyManager.setServerAdapter(serverAdapter);

            // Register listeners
            serverAdapter.addListener(new LobbyListener(lobbyManager, serverAdapter));
            serverAdapter.addListener(new TankMovementListener(lobbyManager));
            serverAdapter.addListener(new TankShootingListener(lobbyManager));
            serverAdapter.addListener(new SkillUpgradeListener(lobbyManager));
            serverAdapter.addListener(new ClassSelectionListener(lobbyManager));
            serverAdapter.addListener(new RespawnListener(lobbyManager));

            serverAdapter.startServer(PORT_TCP, PORT_UDP);

            Logger.info("Server started. Waiting for connections...");

            // Keep main thread alive
            Thread.currentThread().join();
        } catch (IOException e) {
            Logger.error("Server failed to start.", e);
        } catch (InterruptedException e) {
            Logger.info("Server shutting down.");
        }
    }
}
