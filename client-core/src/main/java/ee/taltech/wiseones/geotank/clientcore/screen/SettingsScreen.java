package ee.taltech.wiseones.geotank.clientcore.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ee.taltech.wiseones.geotank.clientcore.audio.AudioManager;
import ee.taltech.wiseones.geotank.clientcore.config.SettingsStore;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenBackground;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenTheme;
import ee.taltech.wiseones.geotank.clientcore.util.Font;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_HEIGHT;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_WIDTH;

/**
 * Settings screen: master/music/SFX volumes, display (fullscreen + resolution + vsync),
 * HUD toggles (FPS, minimap, chat). Rendered with ShapeRenderer + SpriteBatch.
 * Pass a Runnable "onBack" so the caller decides where to return (TitleScreen from menu,
 * paused ArenaScreen from in-game).
 */
public class SettingsScreen extends ScreenAdapter {
    private static final int[][] RESOLUTIONS = {
            {1280, 720},
            {1600, 900},
            {1920, 1080}
    };

    // Row indices — keep the order explicit so layout + hit testing stay in sync.
    private static final int ROW_MASTER = 0;
    private static final int ROW_MUSIC = 1;
    private static final int ROW_SFX = 2;
    private static final int ROW_FULLSCREEN = 3;
    private static final int ROW_RESOLUTION = 4;
    private static final int ROW_VSYNC = 5;
    private static final int ROW_SHOW_FPS = 6;
    private static final int ROW_SHOW_MINIMAP = 7;
    private static final int ROW_SHOW_CHAT = 8;
    private static final int ROW_HUD_SCALE = 9;
    private static final int ROW_COUNT = 10;

    private static final float HUD_SCALE_MIN = 0.75f;
    private static final float HUD_SCALE_MAX = 2.0f;

    private final AudioManager audioManager;
    private final Runnable onBack;

    private final SpriteBatch spriteBatch = new SpriteBatch();
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final ScreenBackground background = new ScreenBackground();
    // Sized for the 1280×720 design canvas — FitViewport scales the whole
    // screen uniformly, but starting from larger fonts keeps the rendered
    // glyphs sharper at common monitor sizes.
    private final BitmapFont fontTitle = Font.getArialFont(40);
    private final BitmapFont fontMedium = Font.getArialFont(18);
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Viewport viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, new OrthographicCamera());

    private float masterVolume;
    private float musicVolume;
    private float effectsVolume;
    private boolean fullscreen;
    private int resolutionIndex;
    private boolean vsync;
    private boolean showFps;
    private boolean showMinimap;
    private boolean showChat;
    private float hudScale;

    // Layout computed each frame.
    private float controlLeft, controlW;
    private float labelRightX;
    private float rowGap;
    private float topY;
    private final float[] rowY = new float[ROW_COUNT];
    private float sliderH;
    private float checkboxSize;
    private float resButtonSize;

    // Resolution nav geometry
    private float resLeftX, resRightX, resDisplayX, resDisplayW;

    // Back button
    private float backX, backY, backW, backH;

    private enum DragTarget {NONE, MASTER, MUSIC, EFFECTS, HUD_SCALE}

    private DragTarget dragging = DragTarget.NONE;

    public SettingsScreen(AudioManager audioManager, Runnable onBack) {
        this.audioManager = audioManager;
        this.onBack = onBack;

        this.masterVolume = SettingsStore.getMasterVolume();
        this.musicVolume = SettingsStore.getMusicVolume();
        this.effectsVolume = SettingsStore.getEffectsVolume();
        this.fullscreen = SettingsStore.isFullscreen();
        this.resolutionIndex = closestResolutionIndex(
                SettingsStore.getWindowWidth(), SettingsStore.getWindowHeight());
        this.vsync = SettingsStore.isVsync();
        this.showFps = SettingsStore.isShowFps();
        this.showMinimap = SettingsStore.isShowMinimap();
        this.showChat = SettingsStore.isShowChat();
        this.hudScale = SettingsStore.getHudScale();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        background.render();

        viewport.apply();
        float screenW = viewport.getWorldWidth();
        float screenH = viewport.getWorldHeight();

        computeLayout(screenW, screenH);
        handleInput();
        draw(screenW, screenH);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    private void computeLayout(float screenW, float screenH) {
        float centerX = screenW / 2f;
        sliderH = 22f;
        checkboxSize = 30f;
        resButtonSize = 40f;

        // Control band: all interactive widgets align into this horizontal span.
        // Caps at ~33% of the canvas so labels on the left and value text on
        // the right have room to read without crowding.
        controlW = Math.min(420f, screenW * 0.4f);
        controlLeft = centerX - controlW / 2f;
        labelRightX = controlLeft - 24f;

        backW = 220f;
        backH = 52f;

        // Fit 10 rows + title + back between ~top 88% and bottom 6%.
        // titleBottomY is below the title glyphs (BitmapFont.draw puts the
        // glyph top at the y arg, so we drop fontTitle.getLineHeight() below
        // the baseline to clear the descender).
        float titleBaselineY = screenH * 0.92f;
        float titleBottomY = titleBaselineY - fontTitle.getLineHeight();
        float bottomReserve = screenH * 0.06f + backH + 24f;
        float usableTop = titleBottomY - 24f;
        float usableBottom = bottomReserve;
        float totalRowSpan = Math.max(40f, usableTop - usableBottom);
        rowGap = Math.min(64f, totalRowSpan / ROW_COUNT);
        topY = usableTop - sliderH;

        for (int i = 0; i < ROW_COUNT; i++) {
            rowY[i] = topY - rowGap * i;
        }

        // Resolution arrows + display band
        resLeftX = controlLeft;
        resRightX = controlLeft + controlW - resButtonSize;
        resDisplayX = resLeftX + resButtonSize + 8f;
        resDisplayW = resRightX - resDisplayX - 8f;

        // Back button centered below last row
        backX = centerX - backW / 2f;
        backY = screenH * 0.06f;
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            onBack.run();
            return;
        }

        Vector2 mouse = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        float mx = mouse.x;
        float my = mouse.y;

        // Drag handling for sliders (continuous while held)
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (dragging == DragTarget.NONE) {
                if (isInSlider(mx, my, rowY[ROW_MASTER])) {
                    dragging = DragTarget.MASTER;
                } else if (isInSlider(mx, my, rowY[ROW_MUSIC])) {
                    dragging = DragTarget.MUSIC;
                } else if (isInSlider(mx, my, rowY[ROW_SFX])) {
                    dragging = DragTarget.EFFECTS;
                } else if (isInSlider(mx, my, rowY[ROW_HUD_SCALE])) {
                    dragging = DragTarget.HUD_SCALE;
                }
            }
            float frac = clamp01((mx - controlLeft) / controlW);
            if (dragging == DragTarget.MASTER) setMasterVolume(frac);
            else if (dragging == DragTarget.MUSIC) setMusicVolume(frac);
            else if (dragging == DragTarget.EFFECTS) setEffectsVolume(frac);
            else if (dragging == DragTarget.HUD_SCALE) setHudScale(
                    HUD_SCALE_MIN + frac * (HUD_SCALE_MAX - HUD_SCALE_MIN));
        } else {
            dragging = DragTarget.NONE;
        }

        if (Gdx.input.justTouched()) {
            // Fullscreen checkbox
            if (inCheckbox(mx, my, rowY[ROW_FULLSCREEN])) {
                toggleFullscreen();
            }
            // Resolution prev/next (only when windowed)
            if (!fullscreen && mx >= resLeftX && mx <= resLeftX + resButtonSize
                    && my >= rowY[ROW_RESOLUTION] && my <= rowY[ROW_RESOLUTION] + resButtonSize) {
                cycleResolution(-1);
            }
            if (!fullscreen && mx >= resRightX && mx <= resRightX + resButtonSize
                    && my >= rowY[ROW_RESOLUTION] && my <= rowY[ROW_RESOLUTION] + resButtonSize) {
                cycleResolution(+1);
            }
            // VSync
            if (inCheckbox(mx, my, rowY[ROW_VSYNC])) {
                vsync = !vsync;
                SettingsStore.setVsync(vsync);
                Gdx.graphics.setVSync(vsync);
            }
            // Show FPS
            if (inCheckbox(mx, my, rowY[ROW_SHOW_FPS])) {
                showFps = !showFps;
                SettingsStore.setShowFps(showFps);
            }
            // Show minimap
            if (inCheckbox(mx, my, rowY[ROW_SHOW_MINIMAP])) {
                showMinimap = !showMinimap;
                SettingsStore.setShowMinimap(showMinimap);
            }
            // Show chat
            if (inCheckbox(mx, my, rowY[ROW_SHOW_CHAT])) {
                showChat = !showChat;
                SettingsStore.setShowChat(showChat);
            }
            // Back button
            if (mx >= backX && mx <= backX + backW && my >= backY && my <= backY + backH) {
                onBack.run();
            }
        }
    }

    private boolean inCheckbox(float mx, float my, float sy) {
        return mx >= controlLeft && mx <= controlLeft + checkboxSize
                && my >= sy && my <= sy + checkboxSize;
    }

    private boolean isInSlider(float mx, float my, float sy) {
        float padY = 10f; // more forgiving hit area than the visible bar
        return mx >= controlLeft - 5f && mx <= controlLeft + controlW + 5f
                && my >= sy - padY && my <= sy + sliderH + padY;
    }

    private void setMasterVolume(float v) {
        if (Math.abs(masterVolume - v) < 0.001f) return;
        masterVolume = v;
        audioManager.setMasterVolume(v);
        SettingsStore.setMasterVolume(v);
    }

    private void setMusicVolume(float v) {
        if (Math.abs(musicVolume - v) < 0.001f) return;
        musicVolume = v;
        audioManager.setMusicVolume(v);
        SettingsStore.setMusicVolume(v);
    }

    private void setEffectsVolume(float v) {
        if (Math.abs(effectsVolume - v) < 0.001f) return;
        effectsVolume = v;
        audioManager.setEffectsVolume(v);
        SettingsStore.setEffectsVolume(v);
    }

    private void setHudScale(float v) {
        if (Math.abs(hudScale - v) < 0.001f) return;
        hudScale = v;
        SettingsStore.setHudScale(v);
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        SettingsStore.setFullscreen(fullscreen);
        applyDisplayMode();
    }

    private void cycleResolution(int delta) {
        resolutionIndex = Math.floorMod(resolutionIndex + delta, RESOLUTIONS.length);
        SettingsStore.setWindow(RESOLUTIONS[resolutionIndex][0], RESOLUTIONS[resolutionIndex][1]);
        applyDisplayMode();
    }

    private void applyDisplayMode() {
        if (fullscreen) {
            Graphics.DisplayMode dm = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(dm);
        } else {
            int w = RESOLUTIONS[resolutionIndex][0];
            int h = RESOLUTIONS[resolutionIndex][1];
            Gdx.graphics.setWindowedMode(w, h);
        }
    }

    private void draw(float screenW, float screenH) {
        // Filled shapes (bars, checkboxes, buttons)
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        shapeRenderer.updateMatrices();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawSliderBar(rowY[ROW_MASTER], masterVolume);
        drawSliderBar(rowY[ROW_MUSIC], musicVolume);
        drawSliderBar(rowY[ROW_SFX], effectsVolume);
        drawCheckbox(rowY[ROW_FULLSCREEN], fullscreen);
        drawResolutionButtons();
        drawCheckbox(rowY[ROW_VSYNC], vsync);
        drawCheckbox(rowY[ROW_SHOW_FPS], showFps);
        drawCheckbox(rowY[ROW_SHOW_MINIMAP], showMinimap);
        drawCheckbox(rowY[ROW_SHOW_CHAT], showChat);
        drawSliderBar(rowY[ROW_HUD_SCALE], hudScaleFrac());
        drawBackButton();
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        drawSliderOutline(rowY[ROW_MASTER]);
        drawSliderOutline(rowY[ROW_MUSIC]);
        drawSliderOutline(rowY[ROW_SFX]);
        drawCheckboxOutline(rowY[ROW_FULLSCREEN]);
        drawResolutionOutlines();
        drawCheckboxOutline(rowY[ROW_VSYNC]);
        drawCheckboxOutline(rowY[ROW_SHOW_FPS]);
        drawCheckboxOutline(rowY[ROW_SHOW_MINIMAP]);
        drawCheckboxOutline(rowY[ROW_SHOW_CHAT]);
        drawSliderOutline(rowY[ROW_HUD_SCALE]);
        drawBackOutline();
        shapeRenderer.end();

        // Text
        spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        spriteBatch.begin();
        drawTitle(screenW, screenH);
        drawLabels();
        spriteBatch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // Sunken track surface — slightly darker than panels so controls read as recessed.
    private static final Color CONTROL_TRACK_BG = new Color(0.06f, 0.07f, 0.10f, 0.92f);
    private static final Color CONTROL_DISABLED_BG = new Color(0.10f, 0.10f, 0.12f, 0.80f);
    private static final float THUMB_RADIUS = 13f;

    private void drawSliderBar(float y, float value) {
        // Track
        shapeRenderer.setColor(CONTROL_TRACK_BG);
        shapeRenderer.rect(controlLeft, y, controlW, sliderH);
        // Filled portion — accent color
        shapeRenderer.setColor(ScreenTheme.ACCENT);
        shapeRenderer.rect(controlLeft, y, controlW * value, sliderH);
        // Thumb — circle on the track for a softer, less rectangular feel
        float thumbX = controlLeft + controlW * value;
        float thumbY = y + sliderH / 2f;
        shapeRenderer.setColor(ScreenTheme.TEXT_PRIMARY);
        shapeRenderer.circle(thumbX, thumbY, THUMB_RADIUS);
    }

    /** No outline on sliders — the track + thumb give plenty of visual definition. */
    private void drawSliderOutline(float y) {
        // Intentionally empty — keeping the method so the render order stays explicit.
    }

    private void drawCheckbox(float y, boolean checked) {
        shapeRenderer.setColor(CONTROL_TRACK_BG);
        shapeRenderer.rect(controlLeft, y, checkboxSize, checkboxSize);
        if (checked) {
            // Inset accent square, with a small inset so it reads as "filled in"
            shapeRenderer.setColor(ScreenTheme.ACCENT);
            shapeRenderer.rect(controlLeft + 5f, y + 5f,
                    checkboxSize - 10f, checkboxSize - 10f);
        }
    }

    /** Hairline border to give checkboxes a clear outer edge against the gradient bg. */
    private void drawCheckboxOutline(float y) {
        shapeRenderer.setColor(ScreenTheme.BORDER);
        shapeRenderer.rect(controlLeft, y, checkboxSize, checkboxSize);
    }

    private void drawResolutionButtons() {
        float y = rowY[ROW_RESOLUTION];
        Color arrowBg = fullscreen ? CONTROL_DISABLED_BG : ScreenTheme.SURFACE_HOVER;
        shapeRenderer.setColor(arrowBg);
        shapeRenderer.rect(resLeftX, y, resButtonSize, resButtonSize);
        shapeRenderer.rect(resRightX, y, resButtonSize, resButtonSize);
        shapeRenderer.setColor(CONTROL_TRACK_BG);
        shapeRenderer.rect(resDisplayX, y, resDisplayW, resButtonSize);
    }

    private void drawResolutionOutlines() {
        float y = rowY[ROW_RESOLUTION];
        shapeRenderer.setColor(ScreenTheme.BORDER);
        shapeRenderer.rect(resLeftX, y, resButtonSize, resButtonSize);
        shapeRenderer.rect(resRightX, y, resButtonSize, resButtonSize);
        shapeRenderer.rect(resDisplayX, y, resDisplayW, resButtonSize);
    }

    private void drawBackButton() {
        shapeRenderer.setColor(ScreenTheme.SURFACE_HOVER);
        shapeRenderer.rect(backX, backY, backW, backH);
    }

    /** Soft hairline on the Back button — distinguishes it without dominating. */
    private void drawBackOutline() {
        shapeRenderer.setColor(ScreenTheme.BORDER);
        shapeRenderer.rect(backX, backY, backW, backH);
    }

    private void drawTitle(float screenW, float screenH) {
        String title = "SETTINGS";
        glyphLayout.setText(fontTitle, title);
        fontTitle.setColor(ScreenTheme.ACCENT);
        fontTitle.draw(spriteBatch, title,
                (screenW - glyphLayout.width) / 2f,
                screenH * 0.93f);
    }

    private void drawLabels() {
        Color labelColor = ScreenTheme.TEXT_PRIMARY;
        drawLabelRightAligned("Master Volume", rowY[ROW_MASTER], sliderH, labelColor);
        drawRightValue(pct(masterVolume), rowY[ROW_MASTER], sliderH);

        drawLabelRightAligned("Music Volume", rowY[ROW_MUSIC], sliderH, labelColor);
        drawRightValue(pct(musicVolume), rowY[ROW_MUSIC], sliderH);

        drawLabelRightAligned("SFX Volume", rowY[ROW_SFX], sliderH, labelColor);
        drawRightValue(pct(effectsVolume), rowY[ROW_SFX], sliderH);

        drawLabelRightAligned("Fullscreen", rowY[ROW_FULLSCREEN], checkboxSize, labelColor);

        Color resLabelColor = fullscreen ? ScreenTheme.TEXT_MUTED : labelColor;
        drawLabelRightAligned("Resolution", rowY[ROW_RESOLUTION], resButtonSize, resLabelColor);

        Color arrowColor = fullscreen ? ScreenTheme.TEXT_MUTED : labelColor;
        fontMedium.setColor(arrowColor);
        drawCentered(fontMedium, "<", resLeftX, rowY[ROW_RESOLUTION], resButtonSize, resButtonSize);
        drawCentered(fontMedium, ">", resRightX, rowY[ROW_RESOLUTION], resButtonSize, resButtonSize);
        fontMedium.setColor(fullscreen ? ScreenTheme.TEXT_SECONDARY : labelColor);
        String resText = RESOLUTIONS[resolutionIndex][0] + " x " + RESOLUTIONS[resolutionIndex][1];
        drawCentered(fontMedium, resText, resDisplayX, rowY[ROW_RESOLUTION], resDisplayW, resButtonSize);

        drawLabelRightAligned("VSync", rowY[ROW_VSYNC], checkboxSize, labelColor);
        drawLabelRightAligned("Show FPS", rowY[ROW_SHOW_FPS], checkboxSize, labelColor);
        drawLabelRightAligned("Show Minimap", rowY[ROW_SHOW_MINIMAP], checkboxSize, labelColor);
        drawLabelRightAligned("Chat Log", rowY[ROW_SHOW_CHAT], checkboxSize, labelColor);

        drawLabelRightAligned("HUD Scale", rowY[ROW_HUD_SCALE], sliderH, labelColor);
        drawRightValue(String.format("%.2fx", hudScale), rowY[ROW_HUD_SCALE], sliderH);

        fontMedium.setColor(labelColor);
        drawCentered(fontMedium, "Back", backX, backY, backW, backH);
    }

    private float hudScaleFrac() {
        return clamp01((hudScale - HUD_SCALE_MIN) / (HUD_SCALE_MAX - HUD_SCALE_MIN));
    }

    private static String pct(float v) {
        return String.format("%d%%", (int) (v * 100));
    }

    /**
     * Right-aligns a label at labelRightX, vertically centered within the row.
     * rowH is the control's visual height so the text baseline aligns with the control.
     */
    private void drawLabelRightAligned(String label, float rowY, float rowH, Color color) {
        fontMedium.setColor(color);
        glyphLayout.setText(fontMedium, label);
        float x = labelRightX - glyphLayout.width;
        float y = rowY + (rowH + glyphLayout.height) / 2f;
        fontMedium.draw(spriteBatch, label, x, y);
    }

    private void drawRightValue(String value, float rowY, float rowH) {
        fontMedium.setColor(ScreenTheme.TEXT_PRIMARY);
        glyphLayout.setText(fontMedium, value);
        float x = controlLeft + controlW + 12f;
        float y = rowY + (rowH + glyphLayout.height) / 2f;
        fontMedium.draw(spriteBatch, value, x, y);
    }

    private void drawCentered(BitmapFont font, String text, float boxX, float boxY,
                              float boxW, float boxH) {
        glyphLayout.setText(font, text);
        font.draw(spriteBatch, text,
                boxX + (boxW - glyphLayout.width) / 2f,
                boxY + (boxH + glyphLayout.height) / 2f);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static int closestResolutionIndex(int w, int h) {
        int best = 0;
        long bestDiff = Long.MAX_VALUE;
        for (int i = 0; i < RESOLUTIONS.length; i++) {
            long d = Math.abs((long) RESOLUTIONS[i][0] - w) + Math.abs(RESOLUTIONS[i][1] - h);
            if (d < bestDiff) {
                bestDiff = d;
                best = i;
            }
        }
        return best;
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        shapeRenderer.dispose();
        background.dispose();
        // Fonts come from the Font cache — shared with other screens, do not dispose.
    }
}
