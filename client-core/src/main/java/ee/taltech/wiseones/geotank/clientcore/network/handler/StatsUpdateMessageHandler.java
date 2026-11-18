package ee.taltech.wiseones.geotank.clientcore.network.handler;

import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.shared.message.StatsUpdateMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Handles XP, level and score updates from the server.
 */
public class StatsUpdateMessageHandler implements ClientMessageHandler<StatsUpdateMessage> {
    private final GameStateManager gameStateManager;

    public StatsUpdateMessageHandler(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @Override
    public void handle(StatsUpdateMessage message) {
        gameStateManager.setLatestStatsUpdate(message);
        Logger.info(String.format("Level: %s, XP: %s, Score: %s",
                message.getLevel(), message.getXp(), message.getScore()));
    }
}
