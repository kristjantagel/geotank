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
import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.clientcore.network.NetworkPlayerCommandHandler;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenBackground;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenTheme;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ThemedDrawables;

import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getButton;
import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getGhostButton;
import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getSecondaryButton;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_PLAYERS_CUSTOM_LOBBY;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_PLAYERS_PER_LOBBY;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.MIN_PLAYERS_CUSTOM_LOBBY;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_HEIGHT;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_WIDTH;

/**
 * Create-lobby form. Layout follows a clear top-down read order: title,
 * subtitle (explains the public/private distinction), three labeled fields
 * grouped in a card panel, then primary Create + ghost Back actions.
 *
 * On success the server pushes a LobbyStateMessage and we transition to
 * {@link LobbyScreen}.
 */
public final class CreateLobbyScreen extends ScreenAdapter {
    private static final int MAX_CODE_LENGTH = 12;
    private static final int MAX_LOBBY_NAME_LENGTH = 24;
    private static final int FIELD_HEIGHT = 48;
    private static final int NAME_FIELD_WIDTH = 460;
    private static final int CODE_FIELD_WIDTH = 320;
    private static final long ERROR_DISPLAY_MS = 3000L;
    private static final float FORM_WIDTH = 640f;
    private static final int ACTION_BUTTON_WIDTH = 360;

    private final Game game;
    private final GameStateManager gameStateManager;
    private final NetworkPlayerCommandHandler networkPlayerCommandHandler;
    private final Stage stage;
    private final ScreenBackground background = new ScreenBackground();
    private final Label errorLabel;
    private final TextField nameField;
    private final TextField codeField;
    private int selectedMaxPlayers = MAX_PLAYERS_PER_LOBBY;
    private Label maxPlayersValueLabel;

    public CreateLobbyScreen(Game game,
                             GameStateManager gameStateManager,
                             NetworkPlayerCommandHandler networkPlayerCommandHandler) {
        this.game = game;
        this.gameStateManager = gameStateManager;
        this.networkPlayerCommandHandler = networkPlayerCommandHandler;
        gameStateManager.clearLobbyError();

        stage = new Stage(new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        Label title = LabelComponents.createLabel("CREATE LOBBY",
                ScreenTheme.ACCENT, ScreenTheme.FONT_TITLE);
        Label subtitle = LabelComponents.createLabel(
                "Public lobbies show in the browser, private lobbies join by code",
                ScreenTheme.TEXT_SECONDARY, ScreenTheme.FONT_SUBTITLE);
        errorLabel = LabelComponents.createLabel("", ScreenTheme.ERROR, ScreenTheme.FONT_LABEL);

        nameField = TextFieldComponents.createTextField(
                "Lobby name (optional)",
                NAME_FIELD_WIDTH, FIELD_HEIGHT, MAX_LOBBY_NAME_LENGTH, ScreenTheme.FONT_BODY);
        codeField = TextFieldComponents.createTextField(
                "Leave empty for public",
                CODE_FIELD_WIDTH, FIELD_HEIGHT, MAX_CODE_LENGTH, ScreenTheme.FONT_BODY);

        var createButton = getButton(ScreenTheme.FONT_BUTTON, "Create Lobby", () -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Lobby";
            String code = codeField.getText().trim();
            networkPlayerCommandHandler.onCreateLobby(
                    name, gameStateManager.getUsername(), selectedMaxPlayers,
                    code.isEmpty() ? null : code);
        });
        var backButton = getGhostButton(ScreenTheme.FONT_BUTTON_SMALL, "Back to Lobbies", () -> {
            game.setScreen(new LobbyBrowserScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
        });

        Container<Table> formCard = buildFormCard();

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(40);

        root.add(title).padBottom(6);
        root.row();
        root.add(subtitle).padBottom(28);
        root.row();
        root.add(errorLabel).padBottom(8);
        root.row();
        root.add(formCard).width(FORM_WIDTH).padBottom(24);
        root.row();
        root.add(createButton).width(ACTION_BUTTON_WIDTH).fillX().padBottom(12);
        root.row();
        root.add(backButton).width(ACTION_BUTTON_WIDTH).fillX();

        stage.addActor(root);
        stage.setKeyboardFocus(nameField);
    }

    /**
     * Form fields wrapped in a single panel — visually groups them together
     * so the user reads them as one form rather than three loose rows.
     */
    private Container<Table> buildFormCard() {
        Table form = new Table();
        form.pad(20, 28, 20, 28);
        form.left();

        form.add(buildFieldGroup("LOBBY NAME", nameField, NAME_FIELD_WIDTH)).left().padBottom(14);
        form.row();
        form.add(buildMaxPlayersGroup()).left().padBottom(14);
        form.row();
        form.add(buildFieldGroup("PRIVATE CODE", codeField, CODE_FIELD_WIDTH)).left();

        Container<Table> card = new Container<>(form);
        card.fill();
        card.setBackground(ThemedDrawables.roundedPanel(ScreenTheme.SURFACE, ScreenTheme.RADIUS_LARGE));
        return card;
    }

    /**
     * One field with its small uppercase header label above it. Label sits
     * tight to the field (8 px gap) so they read as a unit.
     */
    private Table buildFieldGroup(String labelText, TextField field, int width) {
        Label label = LabelComponents.createLabel(labelText, ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_LABEL);
        Table group = new Table();
        group.left();
        group.add(label).left().padBottom(8);
        group.row();
        group.add(field).width(width).height(FIELD_HEIGHT);
        return group;
    }

    /**
     * Steppers row for the max-players value. Same uppercase header pattern
     * as the text fields so the form reads consistently top-to-bottom.
     */
    private Table buildMaxPlayersGroup() {
        Label header = LabelComponents.createLabel("MAX PLAYERS",
                ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_LABEL);
        maxPlayersValueLabel = LabelComponents.createLabel(
                String.valueOf(selectedMaxPlayers), ScreenTheme.TEXT_PRIMARY, ScreenTheme.FONT_BUTTON);
        var minusButton = getSecondaryButton(ScreenTheme.FONT_BUTTON_SMALL, "−", () -> adjustMaxPlayers(-1));
        var plusButton = getSecondaryButton(ScreenTheme.FONT_BUTTON_SMALL, "+", () -> adjustMaxPlayers(1));

        Table valueRow = new Table();
        valueRow.add(minusButton).padRight(16);
        valueRow.add(maxPlayersValueLabel).width(54);
        valueRow.add(plusButton).padLeft(16);

        Table group = new Table();
        group.left();
        group.add(header).left().padBottom(8);
        group.row();
        group.add(valueRow).left();
        return group;
    }

    private void adjustMaxPlayers(int delta) {
        int next = selectedMaxPlayers + delta;
        if (next < MIN_PLAYERS_CUSTOM_LOBBY) next = MIN_PLAYERS_CUSTOM_LOBBY;
        if (next > MAX_PLAYERS_CUSTOM_LOBBY) next = MAX_PLAYERS_CUSTOM_LOBBY;
        selectedMaxPlayers = next;
        maxPlayersValueLabel.setText(String.valueOf(selectedMaxPlayers));
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        background.render();

        // Server sends LobbyStateMessage on successful create — hand off to the waiting room.
        if (!gameStateManager.getLobbyPlayers().isEmpty()) {
            game.setScreen(new LobbyScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
            return;
        }

        updateErrorLabel();

        stage.act(delta);
        stage.draw();
    }

    private void updateErrorLabel() {
        String error = gameStateManager.getLobbyError();
        if (error == null) {
            errorLabel.setText("");
            return;
        }
        long age = System.currentTimeMillis() - gameStateManager.getLobbyErrorSetAt();
        if (age > ERROR_DISPLAY_MS) {
            gameStateManager.clearLobbyError();
            errorLabel.setText("");
        } else {
            errorLabel.setText(error);
        }
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
