package ee.taltech.wiseones.geotank.clientcore.model;

import ee.taltech.wiseones.geotank.clientcore.audio.AudioManager;
import ee.taltech.wiseones.geotank.shared.message.GameStateMessage;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import ee.taltech.wiseones.geotank.shared.message.WelcomeMessage;
import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankConfig;
import ee.taltech.wiseones.geotank.shared.message.dto.ProjectileState;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds game info, tanks, bullets, destructibles etc. running
 */
@Getter
@Setter
public class ArenaModel {
    private int playerTankId;
    private AudioManager audioManager;

    private final List<TankModel> tanks = new ArrayList<>();
    private final List<BulletModel> bullets = new ArrayList<>();
    private final List<DestructibleModel> destructibles = new ArrayList<>();

    private TankModel findOrAddTank(int tankId) {
        return tanks.stream()
                .filter(t -> t.getId() == tankId)
                .findFirst()
                .orElseGet(() -> {
                    var newTank = new TankModel(tankId);
                    tanks.add(newTank);
                    return newTank;
                });
    }

    public void update(WelcomeMessage welcomeMessage) {
        playerTankId = welcomeMessage.getTankId();
        welcomeMessage.getPlayerTankConfigs().forEach(this::update);
    }

    public void update(PlayerTankConfig playerTankConfig) {
        Logger.info("Received PlayerTankConfig: playerId=" + playerTankConfig.getPlayerId() + ", tankId="
                + playerTankConfig.getTankId());
        var tank = findOrAddTank(playerTankConfig.getTankId());
        tank.setHeight(playerTankConfig.getHeight());
        tank.setWidth(playerTankConfig.getWidth());
    }

    public void update(GameStateMessage gameStateMessage) {
        // update tank positions and add new ones
        gameStateMessage.getPlayerTankStates().forEach(tankState -> {
            var tank = findOrAddTank(tankState.getTankId());

            // Audio
            if (tank.getId() == playerTankId && audioManager != null) {
                if (tank.isAlive() && !tankState.isAlive()) audioManager.playDeathSound();
                if (tankState.getLevel() > tank.getLevel()) audioManager.playLevelUpSound();
                if (tank.getSkillPoints() != tankState.getSkillPoints()) audioManager.playUpgradeSound();
            }

            tank.setX(tankState.getX());
            tank.setY(tankState.getY());
            tank.setRotation(tankState.getDirection());
            tank.setLives(tankState.getLives());
            tank.setMaxLives(tankState.getMaxLives());
            tank.setAlive(tankState.isAlive());
            tank.setXp(tankState.getXp());
            tank.setLevel(tankState.getLevel());
            tank.setScore(tankState.getScore());
            tank.setSkillPoints(tankState.getSkillPoints());
            tank.setName(tankState.getName());
            tank.setCannonCount(tankState.getCannonCount());
            tank.setSpreadAngle(tankState.getSpreadAngle());
        });

        updateBullets(gameStateMessage.getProjectileStates());
        updateDestructibles(gameStateMessage.getDestructibleStates());
    }

    public void leavePlayer(int playerId) {
        tanks.removeIf(tank -> tank.getId() == playerId);
    }

    private void updateBullets(List<ProjectileState> projectileStates) {
        bullets.clear();
        for (ProjectileState state : projectileStates) {
            bullets.add(new BulletModel(state));
        }
    }

    private void updateDestructibles(
            List<ee.taltech.wiseones.geotank.shared.message.dto.DestructibleState> destructibleStates) {
        if (destructibleStates == null) return;

        // Remove destroyed destructibles by ID
        destructibles.removeIf(destructible -> destructibleStates.stream()
                .noneMatch(state -> state.getId() == destructible.getId()));

        // Update existing or add new destructibles
        for (var state : destructibleStates) {
            var existing = destructibles.stream()
                    .filter(destructible -> destructible.getId() == state.getId())
                    .findFirst();

            if (existing.isPresent()) {
                var model = existing.get();
                model.setX(state.getX());
                model.setY(state.getY());
                model.setHealth(state.getHealth());
            } else {
                destructibles.add(new DestructibleModel(state));
            }
        }
    }

    public TankModel getPlayerTank() {
        return
                tanks.stream()
                        .filter(t -> t.getId() == playerTankId)
                        .findFirst()
                        .orElse(null);
    }
}