package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.util.Collection;
import java.util.Random;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Service for spawning players at random positions in the arena.
 * Ensures players don't spawn on top of each other or destructibles.
 */
public class SpawnService {
    private static final float SPAWN_BUFFER = 60f; // Minimum distance from other players and destructibles
    private static final int MAX_SPAWN_ATTEMPTS = 20;

    private final Random random = new Random();

    /**
     * Spawns a player at a random position in the arena.
     * Ensures the spawn position doesn't overlap with other players or destructibles.
     *
     * @param tank          the tank to spawn
     * @param otherPlayers  other players currently in the game (used to avoid overlap)
     * @param destructibles destructibles in the game world (used to avoid overlap)
     */
    public void spawnPlayerRandomly(Tank tank, Collection<Player> otherPlayers,
                                    Collection<Destructible> destructibles) {
        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            float x = ARENA_LOWER_BOUND_X + random.nextFloat() * (ARENA_UPPER_BOUND_X - ARENA_LOWER_BOUND_X);
            float y = ARENA_LOWER_BOUND_Y + random.nextFloat() * (ARENA_UPPER_BOUND_Y - ARENA_LOWER_BOUND_Y);

            // Check if position is valid
            if (isValidSpawnPosition(x, y, tank, otherPlayers, destructibles)) {
                tank.getPhysicsState().setX(x);
                tank.getPhysicsState().setY(y);
                Logger.info("Spawned tank " + tank.getId() + " at (" + x + ", " + y + ")");
                return;
            }
        }

        // Fallback: spawn at a default position if no valid position found
        Logger.warn("Could not find valid spawn position for tank " + tank.getId() + ", using default");
        tank.getPhysicsState().setX((float) ARENA_UPPER_BOUND_X / 2);
        tank.getPhysicsState().setY((float) ARENA_UPPER_BOUND_Y / 2);
    }

    /**
     * Checks if a spawn position is valid (doesn't overlap with other tanks or destructibles).
     */
    private boolean isValidSpawnPosition(float x, float y, Tank tank, Collection<Player> otherPlayers,
                                         Collection<Destructible> destructibles) {
        // Check distance from other players
        for (Player player : otherPlayers) {
            if (player.getTank() == tank || !player.isAlive() || player.getTank() == null) {
                continue;
            }

            float otherX = player.getTank().getPhysicsState().getX();
            float otherY = player.getTank().getPhysicsState().getY();
            float distance = calculateDistance(x, y, otherX, otherY);

            if (distance < SPAWN_BUFFER) {
                return false;
            }
        }

        // Check distance from destructibles
        for (Destructible destructible : destructibles) {
            if (destructible.getDestroyedTime() != 0) {
                continue; // Skip destroyed destructibles
            }

            float distance = calculateDistance(x, y, destructible.getX(), destructible.getY());
            if (distance < SPAWN_BUFFER) {
                return false;
            }
        }

        // Check if within arena bounds
        return x >= ARENA_LOWER_BOUND_X + ARENA_BORDER_PADDING && x <= ARENA_UPPER_BOUND_X - ARENA_BORDER_PADDING &&
                y >= ARENA_LOWER_BOUND_Y + ARENA_BORDER_PADDING && y <= ARENA_UPPER_BOUND_Y - ARENA_BORDER_PADDING;
    }

    /**
     * Calculate distance between two points.
     */
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
