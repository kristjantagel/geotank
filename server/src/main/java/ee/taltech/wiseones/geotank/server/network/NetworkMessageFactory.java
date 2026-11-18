package ee.taltech.wiseones.geotank.server.network;

import ee.taltech.wiseones.geotank.server.domain.model.Destructible;
import ee.taltech.wiseones.geotank.server.domain.model.Player;
import ee.taltech.wiseones.geotank.server.domain.model.physics.PhysicsState;
import ee.taltech.wiseones.geotank.server.domain.model.projectile.Projectile;
import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.message.ClassSelectionPromptMessage;
import ee.taltech.wiseones.geotank.shared.message.GameStateMessage;
import ee.taltech.wiseones.geotank.shared.message.WelcomeMessage;
import ee.taltech.wiseones.geotank.shared.message.dto.DestructibleState;
import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankConfig;
import ee.taltech.wiseones.geotank.shared.message.dto.ProjectileState;
import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankState;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class NetworkMessageFactory {

    private NetworkMessageFactory() {
    }

    public static GameStateMessage createGameStateMessage(
            Collection<Player> allPlayers,
            Collection<Projectile> projectiles,
            Collection<Destructible> destructibles // TEMP
    ) {
        GameStateMessage msg = new GameStateMessage();

        // Current state of tanks
        msg.setPlayerTankStates(
                allPlayers.stream()
                        .map(NetworkMessageFactory::createPlayerTankState)
                        .collect(Collectors.toCollection(ArrayList::new))
        );
        // Current state of projectiles
        msg.setProjectileStates(
                projectiles.stream()
                        .map(NetworkMessageFactory::createProjectileState)
                        .collect(Collectors.toCollection(ArrayList::new))
        );
        // Current state of destructibles
        msg.setDestructibleStates(
                destructibles.stream()
                        .map(NetworkMessageFactory::createDestructibleState)
                        .collect(Collectors.toCollection(ArrayList::new))
        );

        return msg;
    }

    static PlayerTankState createPlayerTankState(Player player) {
        Tank tank = player.getTank();
        PhysicsState state = tank.getPhysicsState();
        int cannonCount = (tank instanceof ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank st) ? st.getCannonCount() : 1;
        float spreadAngle = (tank instanceof ee.taltech.wiseones.geotank.server.domain.model.tank.SimpleTank st) ? st.getSpreadAngle() : 0f;
        return new PlayerTankState(
                tank.getId(),
                state.getX(),
                state.getY(),
                tank.getFacingAngle(),
                player.getLives(),
                player.getMaxHp(),
                player.isAlive(),
                player.getXp(),
                player.getLevel(),
                player.getScore(),
                player.getSkillPoints(),
                player.getName(),
                cannonCount,
                spreadAngle
        );
    }

    static ProjectileState createProjectileState(Projectile projectile) {
        return new ProjectileState(
                0,
                projectile.getPhysicsState().getX(),
                projectile.getPhysicsState().getY(),
                0
        );
    }

    static DestructibleState createDestructibleState(Destructible destructible) {
        return new DestructibleState(
                destructible.getId(),
                destructible.getX(),
                destructible.getY(),
                destructible.getHealth(),
                destructible.getType().getId()
        );
    }

    public static WelcomeMessage createWelcomeMessage(Player newPlayer, List<PlayerTankConfig> playerTankConfigs) {
        return new WelcomeMessage(
                newPlayer.getId(),
                newPlayer.getTank().getId(),
                playerTankConfigs
        );
    }

    public static List<PlayerTankConfig> createPlayerTankConfig(Collection<Player> allPlayers) {
        return
                allPlayers.stream()
                        .map(NetworkMessageFactory::createPlayerTankConfig)
                        .collect(Collectors.toCollection(ArrayList::new));

    }

    public static PlayerTankConfig createPlayerTankConfig(Player player) {
        Tank tank = player.getTank();
        PlayerTankConfig config = new PlayerTankConfig(
                player.getId(),
                tank.getId(),
                tank.getPhysicsProperties().getWidth(),
                tank.getPhysicsProperties().getHeight()
        );
        Logger.debug("Created PlayerTankConfig: playerId=" + config.getPlayerId() + ", tankId=" + config.getTankId());
        return config;
    }

    public static ClassSelectionPromptMessage createClassSelectionPromptMessage(Player player) {
        TankClass primary = player.getClassUpgrade().getPrimaryClass();
        TankClass secondary = player.getClassUpgrade().getSecondaryClass();

        // First selection: 7 single-class options.
        if (primary == TankClass.DEFAULT) {
            TankClass[] all = new TankClass[]{
                    TankClass.ARTILLERY, TankClass.SPEEDSTER, TankClass.MACHINE_GUN,
                    TankClass.TANK, TankClass.STREAM, TankClass.RAMMER, TankClass.SNIPER
            };
            return new ClassSelectionPromptMessage(player.getId(), all, new TankClass[all.length]);
        }

        // Multiclass player: [primary, multiclass-keep, secondary]. Middle card is the existing multiclass.
        if (secondary != null) {
            TankClass[] cards = new TankClass[]{primary, primary, secondary};
            TankClass[] cardSecondaries = new TankClass[]{null, secondary, null};
            return new ClassSelectionPromptMessage(player.getId(), cards, cardSecondaries);
        }

        // Single-class player: [prev, current, next], all single-class visuals.
        TankClass[] cards = new TankClass[]{primary.getPrevious(), primary, primary.getNext()};
        return new ClassSelectionPromptMessage(player.getId(), cards, new TankClass[cards.length]);
    }
}
