package ee.taltech.wiseones.geotank.clientcore.network.handler;

/**
 * Message handler interface for client network messages.
 * Implementations handle specific message types received from the server.
 *
 * @param <T> the message type this handler processes
 */
public interface ClientMessageHandler<T> {
    /**
     * Handle a message received from the server.
     *
     * @param message the message to handle
     */
    void handle(T message);
}
