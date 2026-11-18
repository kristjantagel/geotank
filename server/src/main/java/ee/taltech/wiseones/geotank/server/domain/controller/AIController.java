package ee.taltech.wiseones.geotank.server.domain.controller;

import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import lombok.Setter;

/**
 * AI controller for bot-controlled players.
 * Uses AILogic routine to make decisions.
 */
public class AIController implements Controller {
    private final GameWorld gameWorld;
    @Setter
    private AILogic logic;

    public AIController(GameWorld gameWorld, AILogic logic) {
        this.gameWorld = gameWorld;
        this.logic = logic;
    }

    @Override
    public void update(Tank tank, float timeDeltaMs) {
        if (logic == null) {
            return;
        }

        // Get the current player this tank belongs to (needed to exclude self)
        Player currentPlayer = gameWorld.getPlayerManager().getPlayerByTank(tank);
        if (currentPlayer == null) {
            return;
        }

        // Collect other players, destructibles, and projectiles
        var otherPlayers = gameWorld.getPlayerManager().getAllPlayers();
        var destructibles = gameWorld.getAllDestructibles();
        var projectiles = gameWorld.getAllProjectiles();

        // Execute AI logic
        logic.update(tank, otherPlayers, destructibles, projectiles, timeDeltaMs, gameWorld);
    }
}

