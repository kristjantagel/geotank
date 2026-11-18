package ee.taltech.wiseones.geotank.server.network;

import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import ee.taltech.wiseones.geotank.shared.network.TransportLayer;

import java.io.IOException;

public class ServerAdapter implements NetworkSender {
    private final TransportLayer transport;

    public ServerAdapter(Server server) {
        this.transport = new TransportLayer(null, server);
    }

    public void addListener(Listener listener) {
        if (listener != null) {
            transport.registerListener(listener);
        }
    }

    public void startServer(int tcpPort, int udpPort) throws IOException {
        transport.startServer(tcpPort, udpPort);
    }

    @Override
    public void sendToAllClients(Object message) {
        transport.send(message);
    }

    @Override
    public void sendToClient(int connectionID, Object message) {
        transport.sendToClient(connectionID, message);
    }
}
