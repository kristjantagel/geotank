package ee.taltech.wiseones.geotank.shared.network;

import com.esotericsoftware.kryo.Kryo;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.message.*;
import ee.taltech.wiseones.geotank.shared.message.dto.*;

import java.util.ArrayList;

class KryoHelper {
    private KryoHelper() {
    }

    public static void registerClasses(Kryo kryo) {
        // all classes that you want to send over the network
        // must be registered here. To make the handling of these classes
        // easier they are all stored in the "messages" package

        kryo.register(GameJoinMessage.class);
        kryo.register(WelcomeMessage.class);
        kryo.register(GameLeaveMessage.class);

        kryo.register(GameStateMessage.class);
        kryo.register(PlayerShootingMessage.class);
        kryo.register(PlayerTankConfigMessage.class);
        kryo.register(TankMovementMessage.class);
        kryo.register(TankFacingMessage.class);

        kryo.register(Direction.class);
        kryo.register(ProjectileState.class);
        kryo.register(PlayerTankState.class);
        kryo.register(PlayerTankConfig.class);
        kryo.register(StatsUpdateMessage.class);

        kryo.register(ArrayList.class);

        kryo.register(CreateLobbyMessage.class);
        kryo.register(JoinLobbyMessage.class);
        kryo.register(LobbyListRequestMessage.class);
        kryo.register(LobbyListMessage.class);
        kryo.register(PlayerReadyMessage.class);
        kryo.register(LobbyStateMessage.class);
        kryo.register(GameStartMessage.class);
        kryo.register(LobbyPlayerInfo.class);
        kryo.register(LobbyInfo.class);
        kryo.register(DestructibleState.class);
        kryo.register(DestructibleType.class);
        kryo.register(SkillUpgradeMessage.class);
        kryo.register(ChatMessage.class);
        kryo.register(SkillType.class);
        kryo.register(int[].class);

        // Tank class system messages and enum
        kryo.register(ClassSelectionPromptMessage.class);
        kryo.register(ClassSelectionMessage.class);
        kryo.register(TankClass.class);
        kryo.register(TankClass[].class);

        kryo.register(RespawnRequestMessage.class);

        kryo.register(QuickJoinMessage.class);
        kryo.register(JoinByCodeMessage.class);
        kryo.register(LobbyErrorMessage.class);
    }
}
