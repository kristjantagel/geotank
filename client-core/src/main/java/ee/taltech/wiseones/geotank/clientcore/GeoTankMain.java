package ee.taltech.wiseones.geotank.clientcore;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import ee.taltech.wiseones.geotank.clientcore.audio.AudioManager;
import ee.taltech.wiseones.geotank.clientcore.config.SettingsStore;
import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.clientcore.network.ClientAdapter;
import ee.taltech.wiseones.geotank.clientcore.network.NetworkPlayerCommandHandler;
import ee.taltech.wiseones.geotank.clientcore.network.handler.*;
import ee.taltech.wiseones.geotank.clientcore.screen.TitleScreen;
import ee.taltech.wiseones.geotank.shared.message.*;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import lombok.Getter;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Extending the Game class in here is very important for multiple screen
 * support (title screen, lobby screen, etc.) in the game.
 */
public class GeoTankMain extends Game {
    @Getter
    private AudioManager audioManager;

    @Override
    public void create() {
        // Apply persisted display settings before doing anything else so the initial
        // window matches user preference rather than the default launcher size.
        applyStoredDisplaySettings();
        Gdx.graphics.setVSync(SettingsStore.isVsync());

        // Initialize audio manager with persisted volume, then start background music
        audioManager = new AudioManager();
        audioManager.setMasterVolume(SettingsStore.getMasterVolume());
        audioManager.setMusicVolume(SettingsStore.getMusicVolume());
        audioManager.setEffectsVolume(SettingsStore.getEffectsVolume());
        audioManager.startBackgroundMusic();

        GameStateManager gameStateManager = new GameStateManager();
        gameStateManager.setAudioManager(audioManager);

        // Create message dispatcher and register all message handlers
        ClientMessageDispatcher messageDispatcher = new ClientMessageDispatcher(gameStateManager);
        messageDispatcher.registerHandler(GameStateMessage.class, new GameStateMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(WelcomeMessage.class, new WelcomeMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(PlayerTankConfigMessage.class
                , new PlayerTankConfigMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(GameLeaveMessage.class, new GameLeaveMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(StatsUpdateMessage.class, new StatsUpdateMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(LobbyStateMessage.class, new LobbyStateMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(LobbyListMessage.class, new LobbyListMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(GameStartMessage.class, new GameStartMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(ClassSelectionPromptMessage.class,
                new ClassSelectionPromptMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(ChatMessage.class, new ChatMessageHandler(gameStateManager));
        messageDispatcher.registerHandler(LobbyErrorMessage.class,
                msg -> gameStateManager.setLobbyError(msg.getReason()));

        // Increase buffer sizes for UDP to handle large GameStateMessage with many projectiles/destructibles
        // Default was too small
        Client client = new Client(WRITE_BUFFER_SIZE, WRITE_BUFFER_SIZE);
        ClientAdapter clientAdapter = new ClientAdapter(client, messageDispatcher);

        // establish connection to the server
        try {
            Logger.info("Connecting to server at " + SERVER_IP + ":" + PORT_TCP);
            clientAdapter.connectClientToServer(SERVER_IP, PORT_TCP, PORT_UDP);
        } catch (Exception e) {
            Logger.error("Failed to connect to server", e);
            Gdx.app.error("ServerConnection", "Failed to connect to server", e);
            gameStateManager.setDisconnected();
        }

        // display the title screen to the user
        setScreen(new TitleScreen(this, gameStateManager, new NetworkPlayerCommandHandler(clientAdapter)));
    }

    private void applyStoredDisplaySettings() {
        if (SettingsStore.isFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(
                    SettingsStore.getWindowWidth(),
                    SettingsStore.getWindowHeight());
        }
    }

    @Override
    public void dispose() {
        if (audioManager != null) {
            audioManager.dispose();
        }
        super.dispose();
    }
}
