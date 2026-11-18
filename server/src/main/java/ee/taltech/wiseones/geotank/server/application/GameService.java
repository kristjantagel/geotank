package ee.taltech.wiseones.geotank.server.application;

import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.service.AIClassSelectionService;
import ee.taltech.wiseones.geotank.server.domain.service.PlayerManager;
import ee.taltech.wiseones.geotank.server.network.NetworkMessageFactory;
import ee.taltech.wiseones.geotank.server.network.NetworkSender;
import ee.taltech.wiseones.geotank.shared.message.GameLeaveMessage;
import ee.taltech.wiseones.geotank.shared.message.PlayerTankConfigMessage;
import ee.taltech.wiseones.geotank.shared.message.WelcomeMessage;
import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankConfig;
import ee.taltech.wiseones.geotank.shared.message.StatsUpdateMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.AI_RECONCILE_INTERVAL_MS;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.GAME_TICK_RATE;

public class GameService implements Runnable {
    private static final int TICK_RATE_MS = 1000 / GAME_TICK_RATE;

    private final GameWorld gameWorld;
    private final PlayerManager playerManager;
    private final NetworkSender networkSender;
    private final AIClassSelectionService aiClassSelectionService;
    private final Runnable aiReconciler;
    private long lastAIReconcileMs;

    public GameService(GameWorld gameWorld, NetworkSender networkSender) {
        this(gameWorld, networkSender, null);
    }

    public GameService(GameWorld gameWorld, NetworkSender networkSender, Runnable aiReconciler) {
        this.gameWorld = gameWorld;
        this.playerManager = gameWorld.getPlayerManager();
        this.networkSender = networkSender;
        this.aiClassSelectionService = new AIClassSelectionService();
        this.aiReconciler = aiReconciler;
        this.lastAIReconcileMs = 0;
    }

    @Override
    public void run() {
        while (true) {
            long start = System.currentTimeMillis();

            // 0. Periodic AI reconcile (throttled)
            if (aiReconciler != null && start - lastAIReconcileMs >= AI_RECONCILE_INTERVAL_MS) {
                lastAIReconcileMs = start;
                aiReconciler.run();
            }

            // 1. Tick update
            gameWorld.update(TICK_RATE_MS);

            // 1.2 Send welcome msg to new players
            sendWelcomeToNewPlayers();

            // 1.3 Send new player info to all clients
            sendNewPlayers();

            // 1.4 Send leaved players info to all clients
            sendLeavedPlayers();

            // 1.5 Send updates to all clients
            networkSender.sendToAllClients(NetworkMessageFactory.createGameStateMessage(
                    playerManager.getAllPlayers(),
                    gameWorld.getAllProjectiles(),
                    gameWorld.getAllDestructibles()));

            // 1.6 Send updates for stats to players
            Set<Integer> updatedPlayers = new HashSet<>(playerManager.getPlayersWithUpdates());
            playerManager.getPlayersWithUpdates().clear();
            for (Integer i : updatedPlayers) {
                Player player = playerManager.getPlayerById(i);
                if (player == null) continue;
                int connectionId = playerManager.getConnectionIdByPlayerId(i);
                if (connectionId != -1) {
                    networkSender.sendToClient(connectionId, new StatsUpdateMessage(
                            player.getId(), player.getLevel(), player.getXp(), player.getScore(),
                            player.getSkillPoints(), player.getSkillLevelsArray(),
                            player.getClassUpgrade().getPrimaryClass(),
                            player.getClassUpgrade().getSecondaryClass()));

                    // Check if player needs class selection
                    if (player.isNeedsClassSelection()) {
                        networkSender.sendToClient(connectionId,
                                NetworkMessageFactory.createClassSelectionPromptMessage(player));
                    }
                } else {
                    // For AI players (not connected), handle class selection automatically
                    aiClassSelectionService.handleAIClassUpgrade(player);
                }
            }
            playerManager.getPlayersWithUpdates().clear();

            // 2. Tick rate control
            if (!isTrySleep(start))
                break;
        }
    }

    private void sendWelcomeToNewPlayers() {
        if (!playerManager.getNewConnectionIds().isEmpty()) {
            List<PlayerTankConfig> playerTankConfigs =
                    NetworkMessageFactory.createPlayerTankConfig(playerManager.getAllPlayers());
            for (Integer connectionId : playerManager.getNewConnectionIds()) {
                Player player = playerManager.getPlayerByConnectionId(connectionId);
                if (player == null) {
                    playerManager.notNewConnection(connectionId);
                    continue;
                }
                WelcomeMessage welcomeMessage = NetworkMessageFactory
                        .createWelcomeMessage(player, playerTankConfigs);
                networkSender.sendToClient(connectionId, welcomeMessage);
                playerManager.notNewConnection(connectionId);
            }
        }
    }

    private void sendNewPlayers() {
        for (Integer playerId : playerManager.getNewPlayerIds()) {
            Player player = playerManager.getPlayerById(playerId);
            // A disconnect or AI despawn on another thread can wipe the player from
            // playersById between getNewPlayerIds() and getPlayerById(). Drop the stale
            // new-player marker so we don't NPE inside NetworkMessageFactory.
            if (player == null) {
                playerManager.notNewPlayer(playerId);
                continue;
            }
            PlayerTankConfigMessage configMessage = new PlayerTankConfigMessage(
                    NetworkMessageFactory.createPlayerTankConfig(player)
            );
            networkSender.sendToAllClients(configMessage);
            playerManager.notNewPlayer(playerId);
        }
    }

    private void sendLeavedPlayers() {
        for (Integer playerId : playerManager.getLeavedPlayerIds()) {
            GameLeaveMessage gameLeaveMessage = new GameLeaveMessage(playerId);
            networkSender.sendToAllClients(gameLeaveMessage);
            playerManager.notLeavedPlayer(playerId);
        }
    }

    private boolean isTrySleep(long start) {
        long sleep = TICK_RATE_MS - (System.currentTimeMillis() - start);
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }
}
