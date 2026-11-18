package ee.taltech.wiseones.geotank.clientcore.network;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.wiseones.geotank.shared.network.TransportLayer;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.io.IOException;

public class ClientAdapter implements NetworkSender {
    private final TransportLayer transport;

    public ClientAdapter(Client client, Listener listener) {
        this.transport = new TransportLayer(client, null);
        transport.registerListener(listener);
    }

    @Override
    public void sendToServer(Object message) {
        Logger.debug("Sending message to server: " + message.getClass().getSimpleName());
        transport.send(message);
    }

    public void connectClientToServer(String serverIp, int tcpPort, int udpPort) throws IOException {
        Logger.info("Attempting to connect to " + serverIp + ":" + tcpPort);
        transport.connectClientToServer(serverIp, tcpPort, udpPort);
        Logger.info("Successfully connected to server");
    }
}
