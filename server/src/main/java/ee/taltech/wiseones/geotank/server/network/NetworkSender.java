package ee.taltech.wiseones.geotank.server.network;

public interface NetworkSender {
    void sendToAllClients(Object message);

    void sendToClient(int connectionID, Object message);
}
