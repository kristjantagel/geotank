package ee.taltech.wiseones.geotank.clientcore.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
import ee.taltech.wiseones.geotank.shared.message.dto.LobbyInfo;

import java.util.List;

import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getButton;
import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getGhostButton;
import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getSecondaryButton;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_HEIGHT;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_WIDTH;

/**
 * Lobby browser. The visual hierarchy is: title → list of available
 * lobbies (the main content) → row of primary actions (Quick Join, Create
 * Lobby) → secondary "Join by Code" path → ghost Back link.
 * <p>
 * The lobby list is wrapped in a ScrollPane so a busy server with many
 * lobbies still fits the viewport — the list scrolls inside a fixed-height
 * area instead of pushing the action buttons off the bottom of the screen.
 */
public final class LobbyBrowserScreen extends ScreenAdapter {
    private static final int MAX_CODE_LENGTH = 12;
    private static final long ERROR_DISPLAY_MS = 3000L;
    private static final int CODE_FIELD_WIDTH = 280;
    private static final int CODE_FIELD_HEIGHT = 48;
    private static final float REFRESH_INTERVAL = 2.0f;

    // Lobby list spans 60% of the canvas width — wide enough to read at a
    // glance, leaves room for vertical breathing space on the sides.
    // Visible height is tuned to fit the rest of the screen (header + actions
    // + code row + back button) into the 720-px design canvas.
    private static final float LIST_WIDTH = 760f;
    private static final float LIST_VISIBLE_HEIGHT = 220f;
    private static final float CARD_HEIGHT = 72f;
    private static final int PRIMARY_BUTTON_WIDTH = 280;

    private final Game game;
    private final GameStateManager gameStateManager;
    private final NetworkPlayerCommandHandler networkPlayerCommandHandler;
    private final Stage stage;
    private final ScreenBackground background = new ScreenBackground();
    private final Table lobbyListTable;
    private final TextField joinCodeField;
    private final Label errorLabel;
    private float refreshTimer = 0;
    private List<LobbyInfo> lastRenderedLobbies;

    public LobbyBrowserScreen(Game game,
                              GameStateManager gameStateManager,
                              NetworkPlayerCommandHandler networkPlayerCommandHandler) {
        this.game = game;
        this.gameStateManager = gameStateManager;
        this.networkPlayerCommandHandler = networkPlayerCommandHandler;
        gameStateManager.clearLobbyError();

        stage = new Stage(new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        Label title = LabelComponents.createLabel("LOBBIES",
                ScreenTheme.ACCENT, ScreenTheme.FONT_TITLE);
        Label subtitle = LabelComponents.createLabel("Pick a lobby, jump into a quick match, or create your own",
                ScreenTheme.TEXT_SECONDARY, ScreenTheme.FONT_SUBTITLE);
        errorLabel = LabelComponents.createLabel("", ScreenTheme.ERROR, ScreenTheme.FONT_LABEL);

        lobbyListTable = new Table();
        ScrollPane lobbyScroll = buildScrollPane(lobbyListTable);

        var quickJoinButton = getButton(ScreenTheme.FONT_BUTTON, "Quick Join", () ->
                networkPlayerCommandHandler.onQuickJoin(gameStateManager.getUsername()));
        var createButton = getButton(ScreenTheme.FONT_BUTTON, "Create Lobby", () -> {
            game.setScreen(new CreateLobbyScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
        });

        joinCodeField = TextFieldComponents.createTextField(
                "Enter code", CODE_FIELD_WIDTH, CODE_FIELD_HEIGHT, MAX_CODE_LENGTH, ScreenTheme.FONT_BODY);
        var joinByCodeButton = getSecondaryButton(ScreenTheme.FONT_BUTTON_SMALL, "Join by Code", () -> {
            String code = joinCodeField.getText().trim();
            if (!code.isEmpty()) {
                networkPlayerCommandHandler.onJoinByCode(code, gameStateManager.getUsername());
            }
        });

        var backButton = getGhostButton(ScreenTheme.FONT_BUTTON_SMALL, "Back to Title", () -> {
            game.setScreen(new TitleScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
        });

        Table primaryActions = new Table();
        primaryActions.add(quickJoinButton).width(PRIMARY_BUTTON_WIDTH).fillX().padRight(20);
        primaryActions.add(createButton).width(PRIMARY_BUTTON_WIDTH).fillX();

        Table codeRow = new Table();
        codeRow.add(joinCodeField).width(CODE_FIELD_WIDTH).height(CODE_FIELD_HEIGHT).padRight(14);
        codeRow.add(joinByCodeButton);

        Label haveACode = LabelComponents.createLabel("HAVE A PRIVATE CODE?",
                ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_LABEL);

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(36);

        root.add(title).padBottom(6);
        root.row();
        root.add(subtitle).padBottom(22);
        root.row();
        root.add(errorLabel).padBottom(8);
        root.row();
        root.add(lobbyScroll).width(LIST_WIDTH).height(LIST_VISIBLE_HEIGHT).padBottom(24);
        root.row();
        root.add(primaryActions).padBottom(28);
        root.row();
        root.add(haveACode).padBottom(10);
        root.row();
        root.add(codeRow).padBottom(22);
        root.row();
        root.add(backButton);

        stage.addActor(root);
    }

    /**
     * Wraps the lobby list in a vertical scroll pane so a long list doesn't
     * push the action buttons off-screen. Disables horizontal scroll +
     * fading scroll bars so the layout stays predictable.
     */
    private ScrollPane buildScrollPane(Table content) {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        ScrollPane pane = new ScrollPane(content, style);
        pane.setFadeScrollBars(true);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false);
        return pane;
    }

    @Override
    public void show() {
        networkPlayerCommandHandler.onRequestLobbyList();
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        background.render();

        // Late-join shortcut: server pushed GameStartMessage straight to us
        // (Quick Join / Join by Code into a running game).
        if (gameStateManager.isGameStarting()) {
            gameStateManager.clearLobbyState();
            gameStateManager.clearChatMessages();
            // Do not call clearGameSessionState() here — it would race with the
            // server's WelcomeMessage. ArenaScreen.leaveGame clears it instead.
            game.setScreen(new ArenaScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
            return;
        }

        // We've actually entered a waiting-room lobby — hand off to LobbyScreen.
        if (!gameStateManager.getLobbyPlayers().isEmpty()) {
            game.setScreen(new LobbyScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
            return;
        }

        refreshTimer += delta;
        if (refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0;
            networkPlayerCommandHandler.onRequestLobbyList();
        }

        // Identity-compare on the list reference avoids tearing down hovered
        // buttons mid-click; GameStateManager swaps the reference only on a new list.
        List<LobbyInfo> currentLobbies = gameStateManager.getLobbyInfoList();
        if (currentLobbies != lastRenderedLobbies) {
            lastRenderedLobbies = currentLobbies;
            rebuildLobbyList();
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

    private void rebuildLobbyList() {
        lobbyListTable.clear();
        List<LobbyInfo> lobbies = gameStateManager.getLobbyInfoList();

        if (lobbies.isEmpty()) {
            lobbyListTable.add(buildEmptyState()).width(LIST_WIDTH);
            lobbyListTable.row();
            return;
        }

        for (int i = 0; i < lobbies.size(); i++) {
            LobbyInfo lobby = lobbies.get(i);
            lobbyListTable.add(buildLobbyCard(lobby))
                    .width(LIST_WIDTH)
                    .padBottom(i == lobbies.size() - 1 ? 0 : 12);
            lobbyListTable.row();
        }
    }

    /**
     * Build one lobby row as a themed card — name on the left, status meta
     * pill in the middle, join button on the right. Disabled (full and
     * in-game) lobbies render muted with a "FULL" tag instead of a button.
     */
    private Container<Table> buildLobbyCard(LobbyInfo lobby) {
        boolean joinable = lobby.getPlayerCount() < lobby.getMaxPlayers();
        boolean inGame = lobby.isInGame();

        Label name = LabelComponents.createLabel(lobby.getLobbyName(),
                joinable ? ScreenTheme.TEXT_PRIMARY : ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_BUTTON);
        Container<Label> badge = lobbyMetaBadge(lobby);

        Table inner = new Table();
        inner.left().pad(16, 28, 16, 20);
        inner.add(name).left().expandX();
        inner.add(badge).right().padRight(20);

        if (joinable) {
            int lobbyId = lobby.getLobbyId();
            String buttonText = inGame ? "Jump In" : "Join";
            var joinButton = getButton(ScreenTheme.FONT_BUTTON_SMALL, buttonText,
                    () -> networkPlayerCommandHandler.onJoinLobby(lobbyId, gameStateManager.getUsername()));
            inner.add(joinButton).right();
        } else {
            Label fullLabel = LabelComponents.createLabel("FULL",
                    ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_BADGE);
            inner.add(fullLabel).right().padRight(12);
        }

        Container<Table> card = new Container<>(inner);
        card.fill();
        card.minHeight(CARD_HEIGHT);
        card.setBackground(ThemedDrawables.roundedPanel(ScreenTheme.SURFACE, ScreenTheme.RADIUS_LARGE));
        return card;
    }

    /** Pill badge showing player count, with cyan "IN GAME" prefix for running matches. */
    private Container<Label> lobbyMetaBadge(LobbyInfo lobby) {
        boolean inGame = lobby.isInGame();
        String text = inGame
                ? "IN GAME  " + lobby.getPlayerCount() + "/" + lobby.getMaxPlayers()
                : lobby.getPlayerCount() + "/" + lobby.getMaxPlayers();

        Label label = LabelComponents.createLabel(text,
                inGame ? ScreenTheme.SECONDARY : ScreenTheme.TEXT_SECONDARY,
                ScreenTheme.FONT_BADGE);

        Container<Label> pill = new Container<>(label);
        pill.pad(8, 14, 8, 14);
        pill.setBackground(ThemedDrawables.roundedPanel(ScreenTheme.SUNKEN, ScreenTheme.RADIUS_SMALL));
        return pill;
    }

    /** Empty-state card for when the lobby list comes back empty. */
    private Container<Table> buildEmptyState() {
        Label title = LabelComponents.createLabel("No lobbies yet",
                ScreenTheme.TEXT_PRIMARY, ScreenTheme.FONT_BUTTON);
        Label hint = LabelComponents.createLabel("Be the first - create one to get the action started.",
                ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_BODY);

        Table inner = new Table();
        inner.center().pad(28);
        inner.add(title).padBottom(8);
        inner.row();
        inner.add(hint);

        Container<Table> card = new Container<>(inner);
        card.fill();
        card.minHeight(120);
        card.setBackground(ThemedDrawables.roundedPanel(ScreenTheme.SURFACE, ScreenTheme.RADIUS_LARGE));
        return card;
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
