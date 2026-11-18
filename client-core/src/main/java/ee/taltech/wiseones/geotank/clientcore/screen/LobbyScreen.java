package ee.taltech.wiseones.geotank.clientcore.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import ee.taltech.wiseones.geotank.clientcore.component.LabelComponents;
import ee.taltech.wiseones.geotank.clientcore.config.SettingsStore;
import ee.taltech.wiseones.geotank.clientcore.game.GameStateManager;
import ee.taltech.wiseones.geotank.clientcore.network.NetworkPlayerCommandHandler;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenBackground;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenTheme;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ThemedDrawables;
import ee.taltech.wiseones.geotank.shared.message.dto.LobbyPlayerInfo;

import java.util.List;

import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getButton;
import static ee.taltech.wiseones.geotank.clientcore.component.ButtonComponents.getGhostButton;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_MESSAGE_LENGTH;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_HEIGHT;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_WIDTH;

/**
 * Pre-game lobby. Shows the lobby title, a prominent badge for private
 * lobby codes (so the host can easily share it), the player roster as a
 * scrollable list of cards, and the Ready / Leave actions.
 * <p>
 * Chat: press 'C' to start typing, Enter to send, Escape to cancel. Chat
 * input bypasses the Stage so the buttons don't fire while typing.
 */
public final class LobbyScreen extends ScreenAdapter {
    private static final float ROSTER_WIDTH = 640f;
    // Visible roster height tuned to fit title + code badge + roster + actions
    // + hint into the 720-px design canvas.
    private static final float ROSTER_VISIBLE_HEIGHT = 220f;
    private static final float PLAYER_ROW_HEIGHT = 60f;
    private static final int ACTION_BUTTON_WIDTH = 360;

    private final Game game;
    private final GameStateManager gameStateManager;
    private final NetworkPlayerCommandHandler networkPlayerCommandHandler;
    private final Stage stage;
    private final ScreenBackground background = new ScreenBackground();
    private final Table playerListTable;
    private final Label titleLabel;
    private final Label codeBadgeLabel;

    private final StringBuilder chatInput = new StringBuilder();
    private boolean isTypingChat = false;
    private boolean skipNextChar = false;

    // Identity-compare the lobby list so we only rebuild rows when the server
    // pushes a new LobbyStateMessage — avoids reallocating Containers + scene2d
    // children every frame.
    private List<LobbyPlayerInfo> lastRenderedPlayers;

    public LobbyScreen(Game game,
                       GameStateManager gameStateManager,
                       NetworkPlayerCommandHandler networkPlayerCommandHandler) {
        this.game = game;
        this.gameStateManager = gameStateManager;
        this.networkPlayerCommandHandler = networkPlayerCommandHandler;

        stage = new Stage(new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(new ChatAndStageInput());

        titleLabel = LabelComponents.createLabel(titleText(),
                ScreenTheme.ACCENT, ScreenTheme.FONT_TITLE);
        codeBadgeLabel = LabelComponents.createLabel(codeText(),
                ScreenTheme.TEXT_SECONDARY, ScreenTheme.FONT_LABEL);
        Container<Label> codeBadge = wrapAsBadge(codeBadgeLabel);

        Label hint = LabelComponents.createLabel("PRESS C TO CHAT     ENTER TO SEND     ESC TO CANCEL",
                ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_CAPTION);

        var readyButton = getButton(ScreenTheme.FONT_BUTTON, "Ready Up",
                networkPlayerCommandHandler::onToggleReady);
        var leaveButton = getGhostButton(ScreenTheme.FONT_BUTTON_SMALL, "Leave Lobby", () -> {
            networkPlayerCommandHandler.onLeave();
            gameStateManager.clearLobbyState();
            gameStateManager.clearChatMessages();
            game.setScreen(new LobbyBrowserScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
        });

        playerListTable = new Table();
        ScrollPane playerScroll = buildScrollPane(playerListTable);

        Label rosterHeader = LabelComponents.createLabel("PLAYERS",
                ScreenTheme.TEXT_MUTED, ScreenTheme.FONT_LABEL);

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(36);

        root.add(titleLabel).padBottom(10);
        root.row();
        root.add(codeBadge).padBottom(28);
        root.row();
        root.add(rosterHeader).left().padLeft(4).padBottom(8);
        root.row();
        root.add(playerScroll).width(ROSTER_WIDTH).height(ROSTER_VISIBLE_HEIGHT).padBottom(24);
        root.row();
        root.add(readyButton).width(ACTION_BUTTON_WIDTH).fillX().padBottom(12);
        root.row();
        root.add(leaveButton).width(ACTION_BUTTON_WIDTH).fillX().padBottom(22);
        root.row();
        root.add(hint);

        stage.addActor(root);
    }

    private ScrollPane buildScrollPane(Table content) {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        ScrollPane pane = new ScrollPane(content, style);
        pane.setFadeScrollBars(true);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false);
        return pane;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        background.render();

        if (gameStateManager.isGameStarting()) {
            gameStateManager.clearLobbyState();
            gameStateManager.clearChatMessages();
            // Do not call clearGameSessionState() here — it would race with the
            // server's WelcomeMessage. ArenaScreen.leaveGame clears it instead.
            game.setScreen(new ArenaScreen(game, gameStateManager, networkPlayerCommandHandler));
            stage.dispose();
            return;
        }

        // Title and code can change after this screen mounts — race between
        // mounting and LobbyStateMessage arriving — so resync each frame.
        // Player list rebuild only when the reference actually changes.
        List<LobbyPlayerInfo> players = gameStateManager.getLobbyPlayers();
        if (players != lastRenderedPlayers) {
            lastRenderedPlayers = players;
            rebuildPlayerList(players);
        }
        titleLabel.setText(titleText());
        codeBadgeLabel.setText(codeText());

        stage.act(delta);
        stage.draw();
    }

    private String titleText() {
        String name = gameStateManager.getCurrentLobbyName();
        return (name == null || name.isBlank()) ? "LOBBY" : name;
    }

    private String codeText() {
        String code = gameStateManager.getCurrentLobbyCode();
        if (code == null || code.isBlank()) return "PUBLIC LOBBY";
        return "PRIVATE  CODE: " + code;
    }

    private Container<Label> wrapAsBadge(Label inner) {
        Container<Label> badge = new Container<>(inner);
        badge.pad(10, 22, 10, 22);
        badge.setBackground(ThemedDrawables.roundedPanel(ScreenTheme.SURFACE, ScreenTheme.RADIUS_MEDIUM));
        return badge;
    }

    private void rebuildPlayerList(List<LobbyPlayerInfo> players) {
        playerListTable.clear();
        for (int i = 0; i < players.size(); i++) {
            LobbyPlayerInfo player = players.get(i);
            playerListTable.add(buildPlayerCard(player))
                    .width(ROSTER_WIDTH)
                    .padBottom(i == players.size() - 1 ? 0 : 10);
            playerListTable.row();
        }
    }

    private Container<Table> buildPlayerCard(LobbyPlayerInfo player) {
        Label name = LabelComponents.createLabel(player.getPlayerName(),
                ScreenTheme.TEXT_PRIMARY, ScreenTheme.FONT_BUTTON_SMALL);
        Container<Label> readyBadge = readyStatusBadge(player.isReady());

        Table inner = new Table();
        inner.left().pad(14, 28, 14, 18);
        inner.add(name).left().expandX();
        inner.add(readyBadge).right();

        Container<Table> card = new Container<>(inner);
        card.fill();
        card.minHeight(PLAYER_ROW_HEIGHT);
        card.setBackground(ThemedDrawables.roundedPanel(
                player.isReady() ? readyTintedSurface() : ScreenTheme.SURFACE,
                ScreenTheme.RADIUS_LARGE));
        return card;
    }

    /** 12% blend of SURFACE toward SUCCESS — visible without being garish. */
    private static com.badlogic.gdx.graphics.Color readyTintedSurface() {
        com.badlogic.gdx.graphics.Color base = ScreenTheme.SURFACE;
        com.badlogic.gdx.graphics.Color accent = ScreenTheme.SUCCESS;
        float t = 0.12f;
        return new com.badlogic.gdx.graphics.Color(
                base.r + (accent.r - base.r) * t,
                base.g + (accent.g - base.g) * t,
                base.b + (accent.b - base.b) * t,
                base.a);
    }

    private Container<Label> readyStatusBadge(boolean ready) {
        Label label = LabelComponents.createLabel(ready ? "READY" : "WAITING",
                ready ? ScreenTheme.SUCCESS : ScreenTheme.TEXT_MUTED,
                ScreenTheme.FONT_BADGE);
        Container<Label> pill = new Container<>(label);
        pill.pad(7, 16, 7, 16);
        pill.setBackground(ThemedDrawables.roundedPanel(ScreenTheme.SUNKEN, ScreenTheme.RADIUS_SMALL));
        return pill;
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

    /**
     * Wraps the Stage's input but intercepts chat keys so the Stage doesn't
     * receive them while the user is typing — otherwise Enter would trigger
     * focus changes on the Ready button, etc.
     */
    private final class ChatAndStageInput extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.C && !isTypingChat && SettingsStore.isShowChat()) {
                isTypingChat = true;
                skipNextChar = true;
                chatInput.setLength(0);
                return true;
            } else if (keycode == Input.Keys.ESCAPE && isTypingChat) {
                isTypingChat = false;
                chatInput.setLength(0);
                return true;
            } else if (keycode == Input.Keys.ENTER && isTypingChat) {
                String message = chatInput.toString().trim();
                if (!message.isEmpty()) {
                    networkPlayerCommandHandler.onSendChatMessage(gameStateManager.getUsername(), message);
                }
                chatInput.setLength(0);
                isTypingChat = false;
                return true;
            } else if (keycode == Input.Keys.BACKSPACE && isTypingChat) {
                if (!chatInput.isEmpty()) {
                    chatInput.deleteCharAt(chatInput.length() - 1);
                }
                return true;
            }
            return stage.keyDown(keycode);
        }

        @Override
        public boolean keyTyped(char character) {
            if (isTypingChat) {
                if (skipNextChar && (character == 'c' || character == 'C')) {
                    skipNextChar = false;
                    return true;
                }
                skipNextChar = false;

                if (chatInput.length() < MAX_MESSAGE_LENGTH && (Character.isLetterOrDigit(character)
                        || " !?.,:;".indexOf(character) >= 0)) {
                    chatInput.append(character);
                    return true;
                }
            }
            return stage.keyTyped(character);
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            return stage.touchDown(screenX, screenY, pointer, button);
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            stage.touchUp(screenX, screenY, pointer, button);
            return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            stage.mouseMoved(screenX, screenY);
            return false;
        }
    }
}
