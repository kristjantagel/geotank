package ee.taltech.wiseones.geotank.clientcore.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ee.taltech.wiseones.geotank.clientcore.config.SettingsStore;
import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.clientcore.game.PlayerInputManager;
import ee.taltech.wiseones.geotank.clientcore.model.ArenaModel;
import ee.taltech.wiseones.geotank.clientcore.model.DestructibleModel;
import ee.taltech.wiseones.geotank.clientcore.network.NetworkPlayerCommandHandler;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.Hud;
import ee.taltech.wiseones.geotank.clientcore.screen.renderer.ArenaRenderer;
import ee.taltech.wiseones.geotank.clientcore.screen.renderer.RendererRegistry;
import ee.taltech.wiseones.geotank.clientcore.viewmodel.HudViewModel;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.message.GameStateMessage;
import ee.taltech.wiseones.geotank.shared.message.StatsUpdateMessage;
import ee.taltech.wiseones.geotank.shared.message.dto.PlayerTankConfig;
import ee.taltech.wiseones.geotank.clientcore.model.TankModel;
import ee.taltech.wiseones.geotank.shared.util.LevelUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

public class ArenaScreen extends ScreenAdapter {
    private final Game game;
    private final NetworkPlayerCommandHandler networkPlayerCommandHandler;

    private final ArenaRenderer arenaRenderer;
    private final GameStateManager gameStateManager;
    private final PlayerInputManager playerInputManager;

    private final SpriteBatch spriteBatch;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final ArenaModel arena;
    private final Hud hud;

    private boolean paused = false;

    /**
     * Initialize new GameScreen, where the main gameplay happens.
     *
     * @param game Current game instance, used for navigating between screens, for example GameScreen -> TitleScreen
     */
    public ArenaScreen(Game game,
                       GameStateManager gameStateManager,
                       NetworkPlayerCommandHandler networkPlayerCommandHandler) {
        this.game = game;
        this.networkPlayerCommandHandler = networkPlayerCommandHandler;
        this.gameStateManager = gameStateManager;  // receives game state from the server

        arenaRenderer = new ArenaRenderer(new RendererRegistry());
        // listens for user input and sends it to the server
        playerInputManager = new PlayerInputManager(networkPlayerCommandHandler, gameStateManager.getAudioManager());

        spriteBatch = new SpriteBatch();  // where all the content is rendered
        // ExtendViewport keeps VIEWPORT_WIDTH x VIEWPORT_HEIGHT as a minimum visible world
        // area and expands the short axis on larger/wider windows — fills the window with
        // more world instead of stretching the same slice to fit.
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        camera.position.set((float) VIEWPORT_WIDTH / 2, (float) VIEWPORT_HEIGHT / 2, 0);
        camera.update();
        arena = new ArenaModel();  // manages players and bullets
        arena.setAudioManager(gameStateManager.getAudioManager());
        hud = new Hud(spriteBatch);  // info overlay
    }

    /**
     * Render a new frame.
     *
     * @param delta time since rendering the previous frame
     */
    @Override
    public void render(float delta) {
        super.render(delta);
        // clear the screen
        Gdx.gl.glClearColor(UI_BG_COLOR_R, UI_BG_COLOR_G, UI_BG_COLOR_B, UI_BG_COLOR_A);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        processGameStateMessages();
        handlePlayerInput();
        updateCamera();
        renderFrame();
    }

    /**
     * Processes incoming game state messages from the server.
     * The order of message processing is important
     */
    private void processGameStateMessages() {
        // Current player's config
        if (gameStateManager.getWelcomeMessage() != null) {
            arena.update(gameStateManager.getWelcomeMessage());
            gameStateManager.setWelcomeMessage(null);
        }
        // Tank config changes
        if (gameStateManager.hasPlayerTankConfigs()) {
            for (PlayerTankConfig playerTankConfig : gameStateManager.getPlayerTankConfigs()) {
                arena.update(playerTankConfig);
                gameStateManager.remove(playerTankConfig);
            }
        }
        // Player departures
        if (gameStateManager.hasLeavedPLayers()) {
            for (int playerId : gameStateManager.getLeavedPlayers()) {
                arena.leavePlayer(playerId);
                gameStateManager.removeLeavedPlayer(playerId);
            }
        }
    }

    /**
     * Handles all player input: movement, shooting, facing, skills, and navigation.
     */
    private void handlePlayerInput() {
        TankModel localTank = arena.getPlayerTank();

        // ESC toggles pause; handle before any gameplay input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            setPaused(!paused);
        }

        if (paused) {
            handlePauseMenuInput();
            return;
        }

        if (localTank != null && localTank.isAlive()) {
            playerInputManager.handleMovementInput(); // send player movement info to server
            playerInputManager.handleShootingInput(); // send player shooting info to server
            playerInputManager.handleFacingInput(localTank, camera); // send player facing info to server
        }

        // Class selector is non-blocking: movement/shooting continue above.
        // Server sends both primary and (parallel) secondary arrays; multiclass cards
        // carry a non-null secondary. The actual (primary, secondary) tuple to send is
        // resolved by NetworkPlayerCommandHandler from the chosen card + current state.
        TankClass[] classes = gameStateManager.getAvailableClasses();
        TankClass[] secondaries = gameStateManager.getAvailableSecondaryClasses();
        boolean classModalActive = gameStateManager.isClassSelectionAvailable() && classes != null;
        int pickedIdx = -1;
        if (classModalActive) {
            pickedIdx = playerInputManager.pickedClassIndex(classes.length);
            if (pickedIdx < 0 && Gdx.input.justTouched()) {
                int idx = hud.getClassUnderMouse(Gdx.input.getX(), Gdx.input.getY());
                if (idx >= 0 && idx < classes.length) pickedIdx = idx;
            }
        }
        boolean selectionMade = pickedIdx >= 0;
        if (selectionMade) {
            TankClass cardPrimary = classes[pickedIdx];
            TankClass cardSecondary = (secondaries != null && pickedIdx < secondaries.length)
                    ? secondaries[pickedIdx] : null;
            StatsUpdateMessage curStats = gameStateManager.getLatestStatsUpdate();
            TankClass currentPrimary = curStats != null ? curStats.getPrimaryClass() : null;
            TankClass currentSecondary = curStats != null ? curStats.getSecondaryClass() : null;
            networkPlayerCommandHandler.onSelectClass(cardPrimary, cardSecondary, currentPrimary, currentSecondary);
            gameStateManager.setClassSelectionAvailable(false);
            gameStateManager.setAvailableClasses(null);
            gameStateManager.setAvailableSecondaryClasses(null);
            classModalActive = false;
        }

        // The !selectionMade guard prevents the same keystroke from firing a
        // class selection AND a skill upgrade in the same frame —
        // isKeyJustPressed is stable across multiple calls within a frame.
        if (!classModalActive && !selectionMade) {
            handleSkillUpgradeInput();
        }

        // Respawn on SPACE while dead
        if (localTank != null && !localTank.isAlive()
                && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            networkPlayerCommandHandler.onRespawn();
        }
    }

    private void setPaused(boolean value) {
        if (paused == value) return;
        paused = value;
        if (paused) {
            playerInputManager.stopMovement();
        }
    }

    private void handlePauseMenuInput() {
        if (!Gdx.input.justTouched()) return;
        int idx = hud.getPauseButtonUnderMouse(Gdx.input.getX(), Gdx.input.getY());
        if (idx == 0) {
            setPaused(false);
        } else if (idx == 1) {
            openSettings();
        } else if (idx == 2) {
            leaveGame();
        }
    }

    private void openSettings() {
        // Stay paused while in settings, then return to this same ArenaScreen instance
        // on Back so the game resumes cleanly.
        ArenaScreen self = this;
        game.setScreen(new SettingsScreen(
                gameStateManager.getAudioManager(),
                () -> game.setScreen(self)));
    }

    private void leaveGame() {
        networkPlayerCommandHandler.onLeave();
        // Has to happen on the way OUT — clearing on the way IN races with
        // the server's WelcomeMessage which arrives within one tick of
        // GameStartMessage and could be wiped before ArenaScreen reads it.
        gameStateManager.clearGameSessionState();
        // Don't dispose synchronously: leaveGame runs from inside this frame's
        // render() (via handlePauseMenuInput), and the rest of render uses
        // spriteBatch / hud / arenaRenderer.
        game.setScreen(new TitleScreen(game, gameStateManager, networkPlayerCommandHandler));
    }

    /**
     * Updates the camera to follow the local player, clamped to world bounds.
     */
    private void updateCamera() {
        // First update arena with latest game state
        GameStateMessage currentGameState = gameStateManager.getLatestGameStateMessage();
        arena.update(currentGameState);  // update players' and bullets' positions

        // Then move camera to follow player. ExtendViewport resizes camera.viewportWidth/Height
        // to match current window, so clamp against those (not the fixed min dims).
        TankModel camTarget = arena.getPlayerTank();
        if (camTarget != null) {
            float halfW = camera.viewportWidth / 2f;
            float halfH = camera.viewportHeight / 2f;
            // Guard against viewports wider/taller than the arena (extreme window aspect ratios).
            float minX = Math.min(halfW, ARENA_UPPER_BOUND_X / 2f);
            float maxX = Math.max(minX, ARENA_UPPER_BOUND_X - halfW);
            float minY = Math.min(halfH, ARENA_UPPER_BOUND_Y / 2f);
            float maxY = Math.max(minY, ARENA_UPPER_BOUND_Y - halfH);
            camera.position.x = MathUtils.clamp(camTarget.getX(), minX, maxX);
            camera.position.y = MathUtils.clamp(camTarget.getY(), minY, maxY);
        }
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height, false);
    }

    /**
     * Renders the arena and HUD.
     */
    private void renderFrame() {
        // render players and bullets onto the screen (world-space, uses camera projection)
        arenaRenderer.render(arena, spriteBatch, camera);

        // update and render the HUD (screen-space, no camera)
        hud.update(mapToHud(arena));
        hud.render();
    }

    private static final int[] SKILL_KEYS = {
            Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4,
            Input.Keys.NUM_5, Input.Keys.NUM_6, Input.Keys.NUM_7, Input.Keys.NUM_8
    };

    private void handleSkillUpgradeInput() {
        for (int i = 0; i < SKILL_KEYS.length; i++) {
            if (Gdx.input.isKeyJustPressed(SKILL_KEYS[i])) {
                networkPlayerCommandHandler.onSkillUpgrade(i);
            }
        }
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        hud.dispose();
        arenaRenderer.dispose();
    }

    private HudViewModel mapToHud(ArenaModel model) {
        TankModel localTank = model.getPlayerTank();
        int localLives = localTank != null ? localTank.getLives() : 0;
        // Server-authoritative max HP (varies with class upgrades + skill bonuses).
        // Falls back to PLAYER_LIVES_COUNT only before the first state arrives.
        int localMaxLives = (localTank != null && localTank.getMaxLives() > 0)
                ? localTank.getMaxLives() : PLAYER_LIVES_COUNT;
        int localXp = localTank != null ? localTank.getXp() : 0;
        int localLevel = localTank != null ? localTank.getLevel() : 1;
        int localScore = localTank != null ? localTank.getScore() : 0;
        boolean localAlive = localTank != null && localTank.isAlive();

        String statusText = gameStateManager.isConnected() ? "" : "CONNECTION LOST";
        Color statusColor = gameStateManager.isConnected() ? Color.BLACK : Color.RED;

        // Build leaderboard from all tanks, sorted by score descending
        List<HudViewModel.LeaderboardEntry> leaderboard = new ArrayList<>();
        for (TankModel tank : model.getTanks()) {
            boolean isLocal = localTank != null && tank.getId() == localTank.getId();
            String name = tank.getName();
            if (name == null || name.isBlank()) {
                name = "Tank " + tank.getId();
            }
            leaderboard.add(new HudViewModel.LeaderboardEntry(name, tank.getScore(), tank.getLevel(), isLocal));
        }
        leaderboard.sort(Comparator.comparingInt(HudViewModel.LeaderboardEntry::score).reversed());
        if (leaderboard.size() > LEADERBOARD_MAX_ENTRIES) {
            // Trim to top N, then append the local player if they got bumped out so the
            // user always sees their own row.
            HudViewModel.LeaderboardEntry localEntry = null;
            for (HudViewModel.LeaderboardEntry e : leaderboard) {
                if (e.isLocal()) {
                    localEntry = e;
                    break;
                }
            }
            List<HudViewModel.LeaderboardEntry> top = new ArrayList<>(
                    leaderboard.subList(0, LEADERBOARD_MAX_ENTRIES));
            if (localEntry != null && !top.contains(localEntry)) {
                top.add(localEntry);
            }
            leaderboard = top;
        }

        // Get skill levels, skill points, and current class(es) from latest stats update.
        // primary/secondary class come from StatsUpdateMessage so the HUD can show what
        // build the player has chosen — server fires this update right after each
        // class selection, so it stays in sync without polling.
        int[] skillLevels = new int[8];
        int skillPoints = localTank != null ? localTank.getSkillPoints() : 0;
        TankClass primaryClass = null;
        TankClass secondaryClass = null;
        StatsUpdateMessage stats = gameStateManager.getLatestStatsUpdate();
        if (stats != null) {
            if (stats.getSkillLevels() != null) {
                System.arraycopy(stats.getSkillLevels(), 0, skillLevels, 0,
                        Math.min(stats.getSkillLevels().length, 8));
            }
            skillPoints = stats.getSkillPoints();
            primaryClass = stats.getPrimaryClass();
            secondaryClass = stats.getSecondaryClass();
        }

        // Minimap data
        List<HudViewModel.MinimapTank> minimapTanks = new ArrayList<>();
        for (TankModel tank : model.getTanks()) {
            boolean isLocal = localTank != null && tank.getId() == localTank.getId();
            minimapTanks.add(new HudViewModel.MinimapTank(tank.getX(), tank.getY(), isLocal, tank.isAlive()));
        }
        List<HudViewModel.MinimapDestructible> minimapDestructibles = new ArrayList<>();
        for (DestructibleModel d : model.getDestructibles()) {
            minimapDestructibles.add(new HudViewModel.MinimapDestructible(d.getX(), d.getY(), d.getType()));
        }

        return new HudViewModel(
                statusText, statusColor,
                localLives, localMaxLives,
                localXp, localLevel, localScore, skillPoints,
                LevelUtil.xpForCurrentLevel(localLevel), LevelUtil.xpForNextLevel(localLevel),
                skillLevels,
                primaryClass, secondaryClass,
                leaderboard,
                localAlive,
                gameStateManager.isClassSelectionAvailable(),
                gameStateManager.getAvailableClasses(),
                gameStateManager.getAvailableSecondaryClasses(),
                minimapTanks,
                minimapDestructibles,
                paused,
                SettingsStore.isShowMinimap(),
                SettingsStore.isShowFps(),
                Gdx.graphics.getFramesPerSecond()
        );
    }
}
