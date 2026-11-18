package ee.taltech.wiseones.geotank.server.domain.controller;

import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;

import java.util.Collection;

/**
 * Defines the logic routine for AI-controlled players.
 * Can be customized to implement different AI behaviors.
 */
public interface AILogic {
    /**
     * Executes the AI logic to make decisions for the tank.
     *
     * @param tank the AI-controlled tank
     * @param otherPlayers all other players in the game
     * @param destructibles all destructibles in the game world
     * @param projectiles all projectiles in the game world
     * @param timeDeltaMs time delta since last update in milliseconds
     * @param gameWorld context
     */
    void update(Tank tank, Collection<Player> otherPlayers, Collection<Destructible> destructibles,
                Collection<Projectile> projectiles, float timeDeltaMs, GameWorld gameWorld);
}
