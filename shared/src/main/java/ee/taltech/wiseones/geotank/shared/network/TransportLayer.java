package ee.taltech.wiseones.geotank.shared.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import ee.taltech.wiseones.geotank.shared.message.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.CONNECTION_TIMEOUT;

public class TransportLayer {
    private final Client client;
    private final Server server;

    public TransportLayer(Client client, Server server) {
        if (client == null && server == null) {
            throw new IllegalArgumentException("either server or client must be specified");
        }
        if (client != null && server != null) {
            throw new IllegalArgumentException("cannot have both client and server specified at the same time");
        }

        this.client = client;
        this.server = server;

        KryoHelper.registerClasses(getKryo());
    }

    private EndPoint getEndPoint() {
        if (client != null) return client;
        if (server != null) return server;
        throw new IllegalStateException("TransportLayer does not have client or server object!");
    }

    private Kryo getKryo() {
        return getEndPoint().getKryo();
    }

    // Sender (automatic TCP/UDP decision)
    public void send(Object message) {
        boolean reliable = shouldUseTCP(message);

        if (client != null) {
            if (reliable) client.sendTCP(message);
            else client.sendUDP(message);
        } else if (server != null) {
            if (reliable) server.sendToAllTCP(message);
            else server.sendToAllUDP(message);
        }
    }

    public void sendToClient(int connectionID, Object message) {
        Optional<Connection> connectionOpt = Arrays.stream(server.getConnections())
                .filter(conn -> conn.getID() == connectionID)
                .findFirst();
        if (connectionOpt.isEmpty()) {
            Log.error("No such connection ID: " + connectionID);
            return;
        }
        Connection connection = connectionOpt.get();
        if (!connection.isConnected()) {
            Log.error("Connection " + connectionID + " is not alive, message not sent");
            return;
        }
        boolean reliable = shouldUseTCP(message);
        if (reliable) connection.sendTCP(message);
        else connection.sendUDP(message);
    }

    private boolean shouldUseTCP(Object message) {
        return
                message instanceof GameJoinMessage ||
                        message instanceof WelcomeMessage ||
                        message instanceof GameLeaveMessage ||
                        message instanceof PlayerTankConfigMessage ||
                        message instanceof TankMovementMessage ||
                        message instanceof StatsUpdateMessage ||
                        message instanceof SkillUpgradeMessage ||
                        message instanceof CreateLobbyMessage ||
                        message instanceof JoinLobbyMessage ||
                        message instanceof LobbyListRequestMessage ||
                        message instanceof LobbyListMessage ||
                        message instanceof PlayerReadyMessage ||
                        message instanceof LobbyStateMessage ||
                        message instanceof GameStartMessage ||
                        message instanceof ClassSelectionPromptMessage ||
                        message instanceof ClassSelectionMessage ||
                        message instanceof ChatMessage ||
                        message instanceof RespawnRequestMessage ||
                        message instanceof QuickJoinMessage ||
                        message instanceof JoinByCodeMessage ||
                        message instanceof LobbyErrorMessage;
    }

    public void registerListener(Listener listener) {
        getEndPoint().addListener(listener);
    }

    public void startServer(int tcpPort, int udpPort) throws IOException {
        server.start();
        server.bind(tcpPort, udpPort);
    }

    public void connectClientToServer(String serverIp, int tcpPort, int udpPort) throws IOException {
        client.start();
        client.connect(CONNECTION_TIMEOUT, serverIp, tcpPort, udpPort);
    }
}