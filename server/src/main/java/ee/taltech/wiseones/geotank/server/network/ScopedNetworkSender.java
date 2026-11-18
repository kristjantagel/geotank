package ee.taltech.wiseones.geotank.server.network;

import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * A NetworkSender that only sends messages to a specific set of connections.
 * Used to scope game state broadcasts to players within a single game session.
 */
public class ScopedNetworkSender implements NetworkSender {
    private final ServerAdapter serverAdapter;
    private final Set<Integer> connectionIds;

    public ScopedNetworkSender(ServerAdapter serverAdapter, Set<Integer> connectionIds) {
        this.serverAdapter = serverAdapter;
        this.connectionIds = connectionIds;
    }

    @Override
    public void sendToAllClients(Object message) {
        // Defensive copy to avoid ConcurrentModificationException
        for (int connectionId : new HashSet<>(connectionIds)) {
            try {
                serverAdapter.sendToClient(connectionId, message);
            } catch (Exception e) {
                Logger.debug("Failed to send to connection " + connectionId + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void sendToClient(int connectionID, Object message) {
        if (connectionIds.contains(connectionID)) {
            try {
                serverAdapter.sendToClient(connectionID, message);
            } catch (Exception e) {
                Logger.debug("Failed to send to connection " + connectionID + ": " + e.getMessage());
            }
        }
    }
}
