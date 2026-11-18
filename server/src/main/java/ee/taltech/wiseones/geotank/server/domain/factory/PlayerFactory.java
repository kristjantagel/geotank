package ee.taltech.wiseones.geotank.server.domain.factory;

import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.controller.AIController;
import ee.taltech.wiseones.geotank.server.domain.controller.StrategyBasedAILogic;
import ee.taltech.wiseones.geotank.server.domain.controller.NetworkController;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.service.PlayerManager;

/**
 * Factory for creating and initializing player instances.
 * Handles complete player setup including tank creation and controller assignment.
 */
public class PlayerFactory {
    private final GameWorld gameWorld;
    private final PlayerManager playerManager;
    private final TankFactory tankFactory;

    public PlayerFactory(GameWorld gameWorld, TankFactory tankFactory) {
        this.gameWorld = gameWorld;
        this.playerManager = gameWorld.getPlayerManager();
        this.tankFactory = tankFactory;
    }

    /**
     * Creates a new player for the given connection with a tank and network controller.
     *
     * @return a fully initialized Player with tank and controller
     */
    public Player createConnectedPlayer(String name) {
        Player player = playerManager.createPlayer();
        player.setName(name != null && !name.isBlank() ? name : "Player " + player.getId());

        // Create and assign tank
        Tank tank = tankFactory.createPlayerTank(player.getId());
        player.setTank(tank);

        // Assign network controller for incoming messages
        player.setController(new NetworkController());

        return player;
    }

    /**
     * Creates a new AI player with a tank and AI controller.
     *
     * @return a fully initialized AI Player with tank and controller
     */
    public Player createAIPlayer() {
        Player player = playerManager.createPlayer();
        player.setName("Bot " + player.getId());

        // Create and assign tank
        Tank tank = tankFactory.createPlayerTank(player.getId());
        player.setTank(tank);

        AIController aiController = new AIController(gameWorld, new StrategyBasedAILogic());
        player.setController(aiController);

        return player;
    }
}
