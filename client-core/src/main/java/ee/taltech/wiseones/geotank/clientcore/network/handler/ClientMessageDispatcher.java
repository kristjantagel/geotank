package ee.taltech.wiseones.geotank.clientcore.network.handler;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Central dispatcher for client network messages.
 * Routes incoming server messages to their corresponding handlers.
 */
public class ClientMessageDispatcher extends Listener {
    private final Map<Class<?>, ClientMessageHandler<?>> messageHandlers = new HashMap<>();
    private final GameStateManager gameStateManager;

    public ClientMessageDispatcher(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    /**
     * Registers a handler for a specific message type.
     *
     * @param messageType the message class to handle
     * @param handler the handler for this message type
     * @param <T> the message type
     */
    public <T> void registerHandler(Class<T> messageType, ClientMessageHandler<T> handler) {
        messageHandlers.put(messageType, handler);
        Logger.debug("Registered handler for message type: " + messageType.getSimpleName());
    }

    /**
     * Called when a message is received from the server.
     * Routes the message to the appropriate registered handler.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void received(Connection connection, Object message) {
        Class<?> messageType = message.getClass();
        ClientMessageHandler<Object> handler = (ClientMessageHandler<Object>) messageHandlers.get(messageType);

        if (handler != null) {
            try {
                Logger.debug("Received message: " + messageType.getSimpleName());
                handler.handle(message);
            } catch (Exception e) {
                Logger.error("Error handling message of type " + messageType.getSimpleName(), e);
            }
        } else {
            Logger.warn("No handler registered for message type: " + messageType.getSimpleName());
        }
    }

    @Override
    public void connected(Connection connection) {
        Logger.info("Connected to server");
    }

    @Override
    public void disconnected(Connection connection) {
        Logger.info("Disconnected from server");
        gameStateManager.setDisconnected();
    }
}
