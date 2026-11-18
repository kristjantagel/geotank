package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.GameWorld;
import ee.taltech.wiseones.geotank.server.domain.controller.AIController;
import ee.taltech.wiseones.geotank.server.domain.factory.PlayerFactory;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.util.ArrayList;
import java.util.List;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.AI_CAP_HUMAN_THRESHOLD;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.AI_CAP_MULTIPLIER_HIGH;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.AI_CAP_MULTIPLIER_LOW;

/**
 * Keeps the AI population in sync with a per-lobby target.
 * Call reconcile() periodically (e.g. once per second from GameService tick).
 */
public class AISpawner {
    private final GameWorld gameWorld;
    private final PlayerFactory playerFactory;
    private final SpawnService spawnService;

    public AISpawner(GameWorld gameWorld, PlayerFactory playerFactory) {
        this.gameWorld = gameWorld;
        this.playerFactory = playerFactory;
        this.spawnService = new SpawnService();
    }

    /**
     * Adjust the AI population so total living players equals targetTotal.
     * Dead AI players are cleaned up. Humans are never removed.
     *
     * @param targetTotal desired total player count (humans + AI)
     * @param humanCount  current live connected human count
     */
    public synchronized void reconcile(int targetTotal, int humanCount) {
        PlayerManager pm = gameWorld.getPlayerManager();

        List<Player> deadAIs = new ArrayList<>();
        List<Player> livingAIs = new ArrayList<>();
        for (Player p : pm.getAllPlayers()) {
            if (!(p.getController() instanceof AIController)) continue;
            if (p.isAlive()) {
                livingAIs.add(p);
            } else {
                deadAIs.add(p);
            }
        }
        for (Player dead : deadAIs) {
            pm.removePlayer(dead);
        }

        int aiCap = aiCapForHumanCount(humanCount);
        int targetAI = Math.clamp((long) targetTotal - humanCount, 0, aiCap);
        int currentAI = livingAIs.size();

        if (currentAI < targetAI) {
            int toAdd = targetAI - currentAI;
            for (int i = 0; i < toAdd; i++) {
                Player ai = playerFactory.createAIPlayer();
                pm.addPlayer(ai);
                spawnService.spawnPlayerRandomly(
                        ai.getTank(), pm.getAllPlayers(), gameWorld.getAllDestructibles());
            }
            Logger.debug("AISpawner: spawned " + toAdd + " AI (human=" + humanCount
                    + ", target=" + targetTotal + ")");
        } else if (currentAI > targetAI) {
            int excess = currentAI - targetAI;
            for (int i = 0; i < excess && i < livingAIs.size(); i++) {
                pm.removePlayer(livingAIs.get(i));
            }
            Logger.debug("AISpawner: removed " + excess + " AI (human=" + humanCount
                    + ", target=" + targetTotal + ")");
        }
    }

    /**
     * Returns the AI count cap given the current human count:
     * <ul>
     *   <li>humanCount ≤ {@code AI_CAP_HUMAN_THRESHOLD} → cap = humanCount × {@code AI_CAP_MULTIPLIER_LOW} (2×)</li>
     *   <li>humanCount &gt; threshold → cap = floor(humanCount × {@code AI_CAP_MULTIPLIER_HIGH}) (1.5×)</li>
     * </ul>
     * This keeps small parties from being drowned in bots while still scaling up with larger lobbies.
     */
    public static int aiCapForHumanCount(int humanCount) {
        if (humanCount <= 0) return 0;
        float multiplier = humanCount <= AI_CAP_HUMAN_THRESHOLD
                ? AI_CAP_MULTIPLIER_LOW
                : AI_CAP_MULTIPLIER_HIGH;
        return (int) Math.floor(humanCount * multiplier);
    }
}
