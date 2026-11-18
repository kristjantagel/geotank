package ee.taltech.wiseones.geotank.clientcore.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.viewport.FitViewport;
import ee.taltech.wiseones.geotank.clientcore.component.LabelComponents;
import ee.taltech.wiseones.geotank.clientcore.component.TextFieldComponents;
import ee.taltech.wiseones.geotank.clientcore.config.SettingsStore;
import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.clientcore.network.NetworkPlayerCommandHandler;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenBackground;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenTheme;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ThemedDrawables;

import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getButton;
import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getGhostButton;
import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getSecondaryButton;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_USERNAME_LENGTH;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_HEIGHT;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_WIDTH;

/**
 * Main menu — branded hero, username entry, and the three top-level
 * actions (Start, Settings, Exit). Uses a clear visual hierarchy:
 * primary action filled in accent yellow, alternate actions stepped down
 * to secondary and ghost styles so the eye lands on Start first.
 * <p>
 * FitViewport keeps the layout proportional on any window size — a
 * 1920×1080 monitor sees the same composition as 1280×720, just larger.
 */
public final class TitleScreen extends ScreenAdapter {
    private static final int USERNAME_FIELD_WIDTH = 460;
    private static final int USERNAME_FIELD_HEIGHT = 56;
    private static final int BUTTON_WIDTH = 460;
    private static final int ACCENT_LINE_WIDTH = 96;
    private static final int ACCENT_LINE_HEIGHT = 4;

    private final Stage stage;
    private final ScreenBackground background = new ScreenBackground();
    private final TextField usernameField;
    private final Label fieldHint;

    public TitleScreen(Game game,
                       GameStateManager gameStateManager,
                       NetworkPlayerCommandHandler networkPlayerCommandHandler) {
        stage = new Stage(new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        Gdx.input.setInputProcessor(stage);

        Label hero = LabelComponents.createLabel("GEOTANK", ScreenTheme.ACCENT, ScreenTheme.FONT_HERO);
        Container<Label> accentLine = decorativeLine();
        Label tagline = LabelComponents.createLabel("REAL-TIME MULTIPLAYER TANK ARENA",
                ScreenTheme.TEXT_SECONDARY, ScreenTheme.FONT_SUBTITLE);

        Label fieldLabel = LabelComponents.createLabel("USERNAME",
                ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_LABEL);
        usernameField = TextFieldComponents.createTextField("Enter your name",
                USERNAME_FIELD_WIDTH, USERNAME_FIELD_HEIGHT, MAX_USERNAME_LENGTH, ScreenTheme.FONT_BODY);
        usernameField.setText(SettingsStore.getUsername());
        fieldHint = LabelComponents.createLabel("",
                ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_CAPTION);

        // Single source of truth for "start" — clicking the button or pressing
        // Enter while focused on the username field both run the same action.
        Runnable startAction = () -> {
            String finalName = sanitizeUsername(usernameField.getText());
            gameStateManager.setUsername(finalName);
            SettingsStore.setUsername(finalName);
            game.setScreen(new LobbyBrowserScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
        };
        var startButton = getButton(ScreenTheme.FONT_BUTTON, "Start", startAction::run);

        // Hitting Enter in the username field is the standard "submit" affordance
        // and matches the on-screen hint shown next to the field.
        usernameField.setTextFieldListener((field, c) -> {
            if (c == '\n' || c == '\r') {
                startAction.run();
            }
        });
        var settingsButton = getSecondaryButton(ScreenTheme.FONT_BUTTON, "Settings", () ->
                game.setScreen(new SettingsScreen(
                        gameStateManager.getAudioManager(),
                        () -> game.setScreen(new TitleScreen(game, gameStateManager, networkPlayerCommandHandler)))));
        var exitButton = getGhostButton(ScreenTheme.FONT_BUTTON_SMALL, "Exit", Gdx.app::exit);

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        root.add(hero).padBottom(12);
        root.row();
        root.add(accentLine).padBottom(28);
        root.row();
        root.add(tagline).padBottom(72);
        root.row();

        if (!gameStateManager.isConnected()) {
            Label errorLabel = LabelComponents.createLabel("Failed to connect to server",
                    ScreenTheme.ERROR, ScreenTheme.FONT_BODY);
            root.add(errorLabel).padBottom(24);
            root.row();
        }

        root.add(fieldLabel).left().padLeft(4).padBottom(8);
        root.row();
        root.add(usernameField).width(USERNAME_FIELD_WIDTH).height(USERNAME_FIELD_HEIGHT).padBottom(8);
        root.row();
        root.add(fieldHint).left().padLeft(4).padBottom(56);
        root.row();
        // Cell.fillX() makes the button stretch to the cell's allocated width;
        // without it minWidth only reserves space and the button stays small.
        root.add(startButton).width(BUTTON_WIDTH).fillX().padBottom(14);
        root.row();
        root.add(settingsButton).width(BUTTON_WIDTH).fillX().padBottom(14);
        root.row();
        root.add(exitButton).width(BUTTON_WIDTH).fillX();

        stage.addActor(root);
        stage.setKeyboardFocus(usernameField);
    }

    /** Small accent bar under the hero — visual anchor that ties the wordmark to the layout below. */
    private static Container<Label> decorativeLine() {
        Container<Label> container = new Container<>();
        container.setBackground(ThemedDrawables.roundedPanel(ScreenTheme.ACCENT, 1));
        container.width(ACCENT_LINE_WIDTH).height(ACCENT_LINE_HEIGHT);
        return container;
    }

    private static String sanitizeUsername(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_USERNAME_LENGTH);
        }
        return trimmed.isEmpty() ? "Player" : trimmed;
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // Field hint mirrors focus state — implicit affordance for first-time players.
        fieldHint.setText(usernameField.hasKeyboardFocus()
                ? "Press Enter or click Start when you're ready"
                : "Click the field above to type a name");

        background.render();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        background.dispose();
        stage.dispose();
    }
}
