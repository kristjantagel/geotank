package ee.taltech.wiseones.geotank.clientcore.network;

import ee.taltech.wiseones.geotank.shared.message.*;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;

/**
 * Factory for creating network messages to reduce coupling in NetworkPlayerCommandHandler
 */
public class MessageFactory {
    private MessageFactory() {
        /* This utility class should not be instantiated */
    }


    // Movement and control messages
    public static TankMovementMessage createMovementMessage(int directionMask) {
        return new TankMovementMessage(directionMask);
    }

    public static PlayerShootingMessage createShootingMessage() {
        return new PlayerShootingMessage();
    }

    public static TankFacingMessage createFacingMessage(float angleRad) {
        return new TankFacingMessage(angleRad);
    }

    // Game state messages
    public static GameLeaveMessage createGameLeaveMessage() {
        return new GameLeaveMessage();
    }

    public static RespawnRequestMessage createRespawnRequestMessage() {
        return new RespawnRequestMessage();
    }

    // Lobby message
    public static CreateLobbyMessage createCreateLobbyMessage(
            String lobbyName, String playerName, int maxPlayers, String code) {
        return new CreateLobbyMessage(lobbyName, playerName, maxPlayers, code);
    }

    public static QuickJoinMessage createQuickJoinMessage(String playerName) {
        return new QuickJoinMessage(playerName);
    }

    public static JoinByCodeMessage createJoinByCodeMessage(String code, String playerName) {
        return new JoinByCodeMessage(code, playerName);
    }

    public static JoinLobbyMessage createJoinLobbyMessage(int lobbyId, String playerName) {
        return new JoinLobbyMessage(lobbyId, playerName);
    }

    public static LobbyListRequestMessage createLobbyListRequestMessage() {
        return new LobbyListRequestMessage();
    }

    public static PlayerReadyMessage createPlayerReadyMessage() {
        return new PlayerReadyMessage();
    }

    // Upgrade messages
    public static SkillUpgradeMessage createSkillUpgradeMessage(int statIndex) {
        return new SkillUpgradeMessage(statIndex);
    }

    public static ClassSelectionMessage createClassSelectionMessage(TankClass tankClass) {
        return new ClassSelectionMessage(tankClass);
    }

    public static ClassSelectionMessage createClassSelectionMessage(TankClass primary, TankClass secondary) {
        return new ClassSelectionMessage(primary, secondary);
    }

    // Chat messages
    public static ChatMessage createChatMessage(String playerName, String messageText) {
        return new ChatMessage(playerName, messageText);
    }
}
