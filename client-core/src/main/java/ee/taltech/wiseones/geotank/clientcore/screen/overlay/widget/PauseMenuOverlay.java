package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;

/**
 * Center-screen pause menu with stacked buttons.
 *
 * Owns its own hit-testing — {@link #buttonUnderMouse} returns 0 (Resume),
 * 1 (Settings), 2 (Leave game), or -1 if the cursor is elsewhere or the
 * pause overlay isn't active. ArenaScreen forwards mouse clicks here.
 *
 * The hover state and the click state read the same layout helpers so
 * the visual hover ring and the actual click target always agree.
 */
public final class PauseMenuOverlay implements HudOverlay {
    public static final String[] LABELS = {"Resume", "Settings", "Leave game"};

    private static final float BUTTON_W = 260f;
    private static final float BUTTON_H = 44f;
    private static final float BUTTON_GAP = 14f;
    private static final float TITLE_GAP = 38f;

    private static final Color DIM_BG = new Color(0f, 0f, 0f, 0.62f);
    private static final Color BUTTON_BG = new Color(0.12f, 0.13f, 0.16f, 0.95f);
    private static final Color BUTTON_HOVER_BG = new Color(0.22f, 0.24f, 0.28f, 1f);
    private static final Color OUTLINE = new Color(1f, 1f, 1f, 0.55f);

    private final HudResources res;

    public PauseMenuOverlay(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean active(HudFrame frame) {
        return frame.viewModel().paused();
    }

    /**
     * Returns the index of the pause button under the given Gdx-coord (top-down Y)
     * mouse position, or -1 when no button is hit / overlay inactive.
     */
    public int buttonUnderMouse(HudFrame frame, int mouseX, int mouseY) {
        if (!active(frame)) return -1;
        float vx = mouseX / frame.scale();
        float hudY = frame.screenH() - mouseY / frame.scale();
        return buttonUnderHud(frame.screenW(), frame.screenH(), vx, hudY);
    }

    @Override
    public void render(HudFrame frame) {
        float vmx = Gdx.input.getX() / frame.scale();
        float vmy = frame.screenH() - Gdx.input.getY() / frame.scale();
        int hovered = buttonUnderHud(frame.screenW(), frame.screenH(), vmx, vmy);

        res.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        res.shapeRenderer.setColor(DIM_BG);
        res.shapeRenderer.rect(0, 0, frame.screenW(), frame.screenH());

        float bx = buttonX(frame.screenW());
        for (int i = 0; i < LABELS.length; i++) {
            float by = buttonY(frame.screenH(), i);
            res.shapeRenderer.setColor(i == hovered ? BUTTON_HOVER_BG : BUTTON_BG);
            res.shapeRenderer.rect(bx, by, BUTTON_W, BUTTON_H);
        }
        res.shapeRenderer.end();

        res.shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < LABELS.length; i++) {
            float by = buttonY(frame.screenH(), i);
            res.shapeRenderer.setColor(OUTLINE);
            res.shapeRenderer.rect(bx, by, BUTTON_W, BUTTON_H);
        }
        res.shapeRenderer.end();

        res.spriteBatch.begin();
        String title = "PAUSED";
        res.glyphLayout.setText(res.fontLarge, title);
        res.fontLarge.setColor(Color.WHITE);
        float titleY = buttonY(frame.screenH(), 0) + BUTTON_H + TITLE_GAP;
        res.fontLarge.draw(res.spriteBatch, title,
                (frame.screenW() - res.glyphLayout.width) / 2f,
                titleY + res.glyphLayout.height);

        for (int i = 0; i < LABELS.length; i++) {
            float by = buttonY(frame.screenH(), i);
            String label = LABELS[i];
            res.glyphLayout.setText(res.fontMedium, label);
            res.fontMedium.setColor(Color.WHITE);
            res.fontMedium.draw(res.spriteBatch, label,
                    bx + (BUTTON_W - res.glyphLayout.width) / 2f,
                    HudUtil.centeredTextY(by, BUTTON_H, res.glyphLayout.height));
        }
        res.spriteBatch.end();
    }

    /** Y-coordinate (bottom of button i), counted up from the screen bottom. */
    private static float buttonY(float screenH, int i) {
        float centerY = screenH / 2f;
        float totalH = LABELS.length * BUTTON_H + (LABELS.length - 1) * BUTTON_GAP;
        float topY = centerY + totalH / 2f - 10f;
        return topY - (i + 1) * BUTTON_H - i * BUTTON_GAP;
    }

    private static float buttonX(float screenW) {
        return (screenW - BUTTON_W) / 2f;
    }

    private static int buttonUnderHud(float screenW, float screenH, float hudX, float hudY) {
        float bx = buttonX(screenW);
        if (hudX < bx || hudX > bx + BUTTON_W) return -1;
        for (int i = 0; i < LABELS.length; i++) {
            float by = buttonY(screenH, i);
            if (hudY >= by && hudY <= by + BUTTON_H) return i;
        }
        return -1;
    }
}
