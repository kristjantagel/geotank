package ee.taltech.wiseones.geotank.server.domain;

import ee.taltech.wiseones.geotank.server.domain.controller.Controller;
import ee.taltech.wiseones.geotank.server.domain.factory.DestructibleFactory;
import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank;
import ee.taltech.wiseones.geotank.server.domain.service.*;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import lombok.Getter;

import java.util.*;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

import ee.taltech.wiseones.geotank.server.domain.model.physics.Vector2;

/**
 * Holds the game state
 */
public class GameWorld {
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Destructible> destructibles = new ArrayList<>();
    private final List<Destructible> respawningDestructibles = new ArrayList<>();
    @Getter
    private final PlayerManager playerManager = new PlayerManager();
    private final CollisionService collisionService;
    private final TankMovementService tankMovementService;
    private final ShootingService shootingService;
    private final ProjectileMover projectileMover;
    private final Random random = new Random();


    public GameWorld(CollisionService collisionService,
                     TankMovementService tankMovementService,
                     ShootingService shootingService,
                     ProjectileMover projectileMover) {
        this.collisionService = collisionService;
        this.tankMovementService = tankMovementService;
        this.shootingService = shootingService;
        this.projectileMover = projectileMover;
    }

    public synchronized Collection<Projectile> getAllProjectiles() {
        return new ArrayList<>(projectiles); // defensive copy for thread safety
    }

    public synchronized Collection<Destructible> getAllDestructibles() {
        return new ArrayList<>(destructibles); // defensive copy for thread safety
    }

    /**
     * Initializes destructible objects in the game world at random positions.
     */
    public synchronized void initializeDestructibles() {
        int attempts = 0;
        int maxAttempts = DESTRUCTIBLES_PER_ARENA * 10;

        while (destructibles.size() < DESTRUCTIBLES_PER_ARENA && attempts < maxAttempts) {
            float x = random.nextFloat() * (ARENA_UPPER_BOUND_X - DESTRUCTIBLE_WIDTH);
            float y = random.nextFloat() * (ARENA_UPPER_BOUND_Y - DESTRUCTIBLE_HEIGHT);

            // Ensure destructible doesn't spawn too close to player spawn zones (center area)
            destructibles.add(DestructibleFactory.createRandomDestructible(x, y));
            attempts++;
        }
        Logger.info("Initialized " + destructibles.size() + " destructibles");
    }

    /**
     * Main game loop tick. Orchestrates the sequence of updates:
     * Input -> Physics -> Combat -> World State.
     */
    public synchronized void update(float deltaTimeMs) {
        List<Player> allPlayers = new ArrayList<>(playerManager.getAllPlayers());
        List<Player> alivePlayers = allPlayers.stream().filter(Player::isAlive).toList();

        // 1. Inputs and Movement
        updatePlayerControllers(alivePlayers, deltaTimeMs);
        updateTankPhysics(allPlayers, alivePlayers, deltaTimeMs);

        // 2. Combat and Projectiles
        updateHealthAndShooting(alivePlayers, deltaTimeMs);
        updateProjectiles(allPlayers, deltaTimeMs);

        // 3. World State
        handlePlayerDeaths(allPlayers);
        updateDestructibles();
    }

    /**
     * Updates steering and shooting inputs from AI or network controllers.
     */
    private void updatePlayerControllers(List<Player> alivePlayers, float deltaTimeMs) {
        for (Player player : alivePlayers) {
            Controller controller = player.getController();
            if (controller != null) {
                controller.update(player.getTank(), deltaTimeMs);
            }
        }
    }

    /**
     * Handles linear movement and resolves collisions between tanks,
     * destructibles, and world boundaries.
     */
    private void updateTankPhysics(List<Player> allPlayers, List<Player> alivePlayers, float deltaTimeMs) {
        for (Player player : alivePlayers) {
            tankMovementService.move(player.getTank(), deltaTimeMs);
            collisionService.collisionWithBounds(player.getTank());
        }

        collisionService.resolveTankTankCollisions(allPlayers);
        collisionService.resolveTankDestructibleCollisions(allPlayers, destructibles);

        // Final boundary check after displacements
        for (Player player : alivePlayers) {
            collisionService.collisionWithBounds(player.getTank());
        }
    }

    /**
     * Processes passive health regeneration and handles weapon firing logic.
     */
    private void updateHealthAndShooting(List<Player> alivePlayers, float deltaTimeMs) {
        float deltaSeconds = deltaTimeMs / 1000;
        for (Player player : alivePlayers) {
            player.updateRegen(deltaSeconds);
            shootingService.update(player.getTank(), projectiles);
        }
    }

    /**
     * Advances projectile positions and checks for collisions against tanks
     * and destructibles.
     */
    private void updateProjectiles(List<Player> allPlayers, float deltaTimeMs) {
        // Remove projectiles that have exceeded their lifetime
        long currentTimeMs = System.currentTimeMillis();
        projectiles.removeIf(p -> {
            Tank ownerTank = p.getOwner();
            if (ownerTank instanceof SimpleTank simpleTank) {
                long lifeTimeMs = (long) simpleTank.getBulletLifetime();
                long elapsedMs = currentTimeMs - p.getSpawnTime();
                return elapsedMs > lifeTimeMs;
            }
            return false;
        });

        List<Projectile> currentProjectiles = new ArrayList<>(projectiles);
        for (Projectile projectile : currentProjectiles) {
            projectileMover.move(projectile, deltaTimeMs);
        }

        // Handle bullet-to-tank collisions
        projectiles.removeIf(p -> collisionService.isColliding(p, allPlayers));

        // Handle bullet-to-destructible collisions
        handleDestructibleCollisions(currentProjectiles);
    }

    /**
     * Identifies players with zero lives and initiates the death/scoring process.
     */
    private void handlePlayerDeaths(List<Player> allPlayers) {
        for (Player player : allPlayers) {
            if (player.isAlive() && player.getLives() <= 0) {
                processKilledPlayer(player);
            }
        }
    }

    /**
     * Disables a killed player's tank and awards kill XP to the last attacker.
     */
    private void processKilledPlayer(Player victim) {
        victim.setAlive(false);
        var tank = victim.getTank();
        if (!(tank instanceof SimpleTank)) return;
        tank.setDesiredDirection(null);
        tank.setWantsToShoot(false);

        if (victim.getLastHitByTank() != null) {
            Player killer = playerManager.getPlayerByTank(victim.getLastHitByTank());
            if (killer != null && killer.getId() != victim.getId()) {
                int xp = Math.clamp(victim.getScore(), BASE_KILL_XP, MAX_KILL_XP);
                awardReward(killer, xp, "killed player " + victim.getId());
            }
            victim.setLastHitByTank(null);
        }
    }

    /**
     * Adds XP and Score to a player and flags them for a network synchronization update.
     */
    private void awardReward(Player player, int amount, String context) {
        player.addXp(amount);
        player.setScore(player.getScore() + amount);
        playerManager.getPlayersWithUpdates().add(player.getId());
        Logger.debug("Player " + player.getId() + " " + context + ", earned " + amount + " XP");
    }

    /**
     * Scans for projectiles intersecting with world destructibles
     * and triggers hit resolution.
     */
    private void handleDestructibleCollisions(List<Projectile> currentProjectiles) {
        if (destructibles.isEmpty()) return;

        for (Projectile projectile : currentProjectiles) {
            // Create a copy to avoid ConcurrentModificationException while removing
            List<Destructible> targets = new ArrayList<>(destructibles);

            for (Destructible target : targets) {
                if (projectile.getBoundingBox().isIntersecting(target.getBounds())) {
                    if (projectile.hasAlreadyHit(target)) {
                        continue;
                    }
                    resolveDestructibleHit(projectile, target);
                }
            }
        }
    }

    /**
     * Handles the logic for when a projectile successfully impacts a destructible.
     */
    private void resolveDestructibleHit(Projectile projectile, Destructible target) {

        projectile.markHit(target);

        if (target.isTakeDamage(projectile.getDamage())) {
            processDestructibleDestruction(projectile, target);
        }

        if (projectile.isConsumeHit()) {
            projectiles.remove(projectile);
        }
    }

    /**
     * Finalizes destruction of a world object, awarding XP to the shooter and queueing the object for respawn.
     */
    private void processDestructibleDestruction(Projectile projectile, Destructible target) {
        Player shooter = playerManager.getPlayerByTank(projectile.getOwner());
        if (shooter != null) {
            awardReward(shooter, target.getXpReward(), "destroyed " + target.getType().getName());
        }

        respawningDestructibles.add(target);
        destructibles.remove(target);
    }

    /**
     * Manages the respawn timer and arena placement for destroyed world objects.
     */
    private void updateDestructibles() {
        if (respawningDestructibles.isEmpty() || destructibles.size() >= MAX_DESTRUCTIBLES_IN_ARENA) return;

        for (Destructible dest : new ArrayList<>(respawningDestructibles)) {
            if (dest.isTryRespawn(DESTRUCTIBLE_RESPAWN_DELAY_MS)) {
                dest.setX(random.nextFloat() * (ARENA_UPPER_BOUND_X - dest.getWidth()));
                dest.setY(random.nextFloat() * (ARENA_UPPER_BOUND_Y - dest.getHeight()));
                destructibles.add(dest);
                respawningDestructibles.remove(dest);
            }
        }
    }

    /**
     * Respawn a dead player on request. No-op if the player is alive or not found.
     * Player.respawn() wipes XP, score, skills, class state, and the tank's
     * stats; this method then places the freshly-built tank at a random spawn.
     */
    public synchronized void respawnPlayer(int connectionId) {
        Player player = playerManager.getPlayerByConnectionId(connectionId);
        if (player == null || player.isAlive()) return;

        player.respawn();
        player.getTank().getPhysicsState().setX(random.nextFloat() * ARENA_UPPER_BOUND_X);
        player.getTank().getPhysicsState().setY(random.nextFloat() * ARENA_UPPER_BOUND_Y);
        player.getTank().getPhysicsState().setSpeed(new Vector2(0, 0));
        player.getTank().getPhysicsState().setAcceleration(new Vector2(0, 0));

        // Skill levels and class identity ride on StatsUpdateMessage — push one
        // now so the client's HUD reflects the wiped state immediately instead
        // of waiting for the next XP gain.
        playerManager.getPlayersWithUpdates().add(player.getId());
    }
}

