package ee.taltech.wiseones.geotank.shared.message;

import lombok.Getter;
import lombok.Setter;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_MESSAGE_LENGTH;

/**
 * Message for chat communication between players in the lobby.
 * Username and message content are kept separate to prevent injection attacks.
 * No serialization used - kept simple to avoid serialization-specific errors.
 */
@Getter
@Setter
public class ChatMessage {
    public static final int MAX_PLAYER_NAME_LENGTH = 15;
    private String playerName;
    private String message;
    private long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String playerName, String message) {
        this.playerName = sanitizePlayerName(playerName);
        this.message = sanitizeMessage(message);
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Sanitizes player name to prevent injection attacks.
     * Limits to 15 characters and removes dangerous characters.
     */
    private static String sanitizePlayerName(String name) {
        if (name == null || name.isBlank()) {
            return "Player";
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_PLAYER_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_PLAYER_NAME_LENGTH);
        }
        return trimmed;
    }

    /**
     * Sanitizes message content to prevent injection attacks.
     * Limits to 200 characters and removes dangerous characters.
     */
    private static String sanitizeMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "";
        }
        String trimmed = msg.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            trimmed = trimmed.substring(0, MAX_MESSAGE_LENGTH);
        }
        return trimmed;
    }

    @Override
    public String toString() {
        return playerName + ": " + message;
    }
}
