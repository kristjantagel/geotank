package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.config.SettingsStore;
import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.ChatMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

public class ChatMessageHandler implements ClientMessageHandler<ChatMessage> {
    private final GameStateManager gameStateManager;

    public ChatMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(ChatMessage message) {
        gameStateManager.addChatMessage(message);
        if (SettingsStore.isShowChat()) {
            Logger.info("[CHAT] " + message);
        }
    }
}
