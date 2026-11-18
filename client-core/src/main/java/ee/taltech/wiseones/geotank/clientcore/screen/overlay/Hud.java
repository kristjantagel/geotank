package ee.taltech.wiseones.geotank.clientcore.screen.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ee.taltech.wiseones.geotank.clientcore.config.SettingsStore;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.ClassSelectorOverlay;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.DeathOverlay;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.FpsCounterWidget;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.HpBarWidget;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.HudOverlay;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.HudWidget;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.LeaderboardWidget;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.MinimapWidget;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.PauseMenuOverlay;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.SkillPanelWidget;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.StatusTextWidget;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget.XpBarWidget;
import ee.taltech.wiseones.geotank.clientcore.viewmodel.HudViewModel;

import java.util.List;

import static ee.taltech.wiseones.geotank.clientcore.util.Font.getArialFont;

/**
 * Diep.io-style HUD overlay rendered in screen space (not affected by the game camera).
 * <p>
 * Owns the shared GPU resources (batch, shapes, fonts) and orchestrates a list of
 * always-on widgets plus a list of conditional overlays. Each render call:
 * <ol>
 *   <li>computes the virtual canvas (HUD scale lets the player resize the UI)</li>
 *   <li>runs three batched passes — filled shapes, line shapes, text — calling
 *       every visible widget for each pass</li>
 *   <li>draws each active overlay in priority order on top</li>
 * </ol>
 * Widget classes own their own draw + hit-test logic, keeping this file small.
 */
public final class Hud {
    private final HudResources res;

    private final List<HudWidget> widgets;
    private final ClassSelectorOverlay classSelector;
    private final PauseMenuOverlay pauseMenu;
    private final List<HudOverlay> overlays;

    private HudViewModel viewModel;

    public Hud(SpriteBatch spriteBatch) {
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        BitmapFont fontSmall = getArialFont(10);
        BitmapFont fontMedium = getArialFont(14);
        BitmapFont fontLarge = getArialFont(18);
        GlyphLayout glyphLayout = new GlyphLayout();
        this.res = new HudResources(spriteBatch, shapeRenderer, fontSmall, fontMedium, fontLarge, glyphLayout);

        // Always-on widgets, draw order: bars first (behind), text labels naturally on top
        // because the text pass runs after the shape passes.
        this.widgets = List.of(
                new HpBarWidget(res),
                new XpBarWidget(res),
                new LeaderboardWidget(res),
                new SkillPanelWidget(res),
                new MinimapWidget(res),
                new StatusTextWidget(res),
                new FpsCounterWidget(res)
        );

        // Overlays, lowest-priority first. Class selection is non-blocking and
        // shown above gameplay; death overlay below class selector / pause; pause on top.
        this.classSelector = new ClassSelectorOverlay(res);
        this.pauseMenu = new PauseMenuOverlay(res);
        this.overlays = List.of(classSelector, new DeathOverlay(res), pauseMenu);
    }

    public void update(HudViewModel vm) {
        this.viewModel = vm;
    }

    /**
     * Renders the entire HUD in screen space. Must be called after world rendering so
     * the HUD sits on top, and all coordinate math operates on the virtual canvas
     * (real pixels divided by the HUD scale setting).
     */
    public void render() {
        if (viewModel == null) return;

        float scale = hudScale();
        float screenW = Gdx.graphics.getWidth() / scale;
        float screenH = Gdx.graphics.getHeight() / scale;

        res.shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        res.shapeRenderer.updateMatrices();
        res.spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);

        // Semi-transparent panels need alpha blending on.
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        HudFrame frame = new HudFrame(viewModel, screenW, screenH, scale);

        // Pass 1 — filled shapes
        res.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (HudWidget w : widgets) {
            if (w.visible(frame)) w.drawFilled(frame);
        }
        res.shapeRenderer.end();

        // Pass 2 — line outlines
        res.shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (HudWidget w : widgets) {
            if (w.visible(frame)) w.drawOutline(frame);
        }
        res.shapeRenderer.end();

        // Pass 3 — text
        res.spriteBatch.begin();
        for (HudWidget w : widgets) {
            if (w.visible(frame)) w.drawText(frame);
        }
        res.spriteBatch.end();

        for (HudOverlay o : overlays) {
            if (o.active(frame)) o.render(frame);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Hit-test for the class selector cards — forwarded by ArenaScreen on click.
     * Returns the index into the available-classes array, or -1 for no hit.
     */
    public int getClassUnderMouse(int mouseX, int mouseY) {
        if (viewModel == null) return -1;
        HudFrame frame = currentFrame();
        return classSelector.classUnderMouse(frame, mouseX, mouseY);
    }

    /**
     * Hit-test for the pause-menu buttons — forwarded by ArenaScreen on click.
     * Returns 0=Resume, 1=Settings, 2=Leave game, or -1 for no hit / not paused.
     */
    public int getPauseButtonUnderMouse(int mouseX, int mouseY) {
        if (viewModel == null) return -1;
        HudFrame frame = currentFrame();
        return pauseMenu.buttonUnderMouse(frame, mouseX, mouseY);
    }

    /** Snapshot the canvas dims at call time so hit-testing matches what was rendered. */
    private HudFrame currentFrame() {
        float scale = hudScale();
        float screenW = Gdx.graphics.getWidth() / scale;
        float screenH = Gdx.graphics.getHeight() / scale;
        return new HudFrame(viewModel, screenW, screenH, scale);
    }

    /**
     * User-configurable HUD scale from settings, clamped to a sane range.
     * 1.0 = native pixels; 1.5 = 50% larger.
     */
    private float hudScale() {
        float s = SettingsStore.getHudScale();
        return Math.max(0.75f, Math.min(2.0f, s));
    }

    public void dispose() {
        res.shapeRenderer.dispose();
        // Fonts are cached by Font.getArialFont and shared with menu Labels —
        // disposing here would crash subsequent screens that look up the same size.
    }
}
