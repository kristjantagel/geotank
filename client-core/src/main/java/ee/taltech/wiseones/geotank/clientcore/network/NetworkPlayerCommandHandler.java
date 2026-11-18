package ee.taltech.wiseones.geotank.clientcore.network;

import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Forwards user wishes to the network
 */
public class NetworkPlayerCommandHandler {

    private final NetworkSender networkSender;

    public NetworkPlayerCommandHandler(NetworkSender networkSender) {
        this.networkSender = networkSender;
    }

    public void onMove(int directionMask) {
        networkSender.sendToServer(MessageFactory.createMovementMessage(directionMask));
    }

    public void onShoot() {
        networkSender.sendToServer(MessageFactory.createShootingMessage());
    }

    public void onRotate(float angleRad) {
        networkSender.sendToServer(MessageFactory.createFacingMessage(angleRad));
    }

    public void onLeave() {
        networkSender.sendToServer(MessageFactory.createGameLeaveMessage());
    }

    public void onCreateLobby(String lobbyName, String playerName, int maxPlayers, String code) {
        networkSender.sendToServer(MessageFactory.createCreateLobbyMessage(lobbyName, playerName, maxPlayers, code));
    }

    public void onQuickJoin(String playerName) {
        networkSender.sendToServer(MessageFactory.createQuickJoinMessage(playerName));
    }

    public void onJoinByCode(String code, String playerName) {
        networkSender.sendToServer(MessageFactory.createJoinByCodeMessage(code, playerName));
    }

    public void onJoinLobby(int lobbyId, String playerName) {
        networkSender.sendToServer(MessageFactory.createJoinLobbyMessage(lobbyId, playerName));
    }

    public void onRequestLobbyList() {
        networkSender.sendToServer(MessageFactory.createLobbyListRequestMessage());
    }

    public void onToggleReady() {
        networkSender.sendToServer(MessageFactory.createPlayerReadyMessage());
    }

    public void onSkillUpgrade(int statIndex) {
        networkSender.sendToServer(MessageFactory.createSkillUpgradeMessage(statIndex));
    }

    /**
     * Resolve the (primary, secondary) tuple to send based on which card the user clicked
     * and the player's current class state, then send the ClassSelectionMessage.
     * <p>
     * Rules:
     *  - Multiclass card (cardSecondary != null) → send as-is (keeps existing multiclass).
     *  - Single-class card, player has no primary yet → send (cardPrimary, null).
     *  - Single-class card, player currently single-class:
     *      - card == current primary → stay single (cardPrimary, null).
     *      - card != current primary → multiclass with current (currentPrimary, cardPrimary).
     *  - Single-class card, player currently multiclass → drop multiclass, become single (cardPrimary, null).
     */
    public void onSelectClass(TankClass cardPrimary, TankClass cardSecondary,
                              TankClass currentPrimary, TankClass currentSecondary) {
        TankClass sendPrimary;
        TankClass sendSecondary;
        if (cardSecondary != null) {
            sendPrimary = cardPrimary;
            sendSecondary = cardSecondary;
        } else if (currentPrimary == null || currentPrimary == TankClass.DEFAULT) {
            sendPrimary = cardPrimary;
            sendSecondary = null;
        } else if (currentSecondary == null) {
            if (cardPrimary == currentPrimary) {
                sendPrimary = cardPrimary;
                sendSecondary = null;
            } else {
                sendPrimary = currentPrimary;
                sendSecondary = cardPrimary;
            }
        } else {
            sendPrimary = cardPrimary;
            sendSecondary = null;
        }
        Logger.info("[CLIENT] Selected class upgrade: " + sendPrimary.getDisplayName()
                + (sendSecondary != null ? " + " + sendSecondary.getDisplayName() : ""));
        networkSender.sendToServer(MessageFactory.createClassSelectionMessage(sendPrimary, sendSecondary));
    }

    public void onSendChatMessage(String playerName, String messageText) {
        networkSender.sendToServer(MessageFactory.createChatMessage(playerName, messageText));
    }

    public void onRespawn() {
        networkSender.sendToServer(MessageFactory.createRespawnRequestMessage());
    }
}
