package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;

/**
 * Dim full-screen overlay shown after the local player dies.
 *
 * Activates only when the player is dead and no higher-priority overlay
 * (class selection, pause menu) is up — those would cover this anyway, so
 * suppressing it avoids stacking dimmed layers and washing the screen out.
 */
public final class DeathOverlay implements HudOverlay {
    private static final Color DIM = new Color(0f, 0f, 0f, 0.55f);
    private static final Color TITLE_COLOR = new Color(1f, 0.25f, 0.25f, 1f);

    private final HudResources res;

    public DeathOverlay(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean active(HudFrame frame) {
        return !frame.viewModel().localAlive()
                && !frame.viewModel().classSelectionActive()
                && !frame.viewModel().paused();
    }

    @Override
    public void render(HudFrame frame) {
        res.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        res.shapeRenderer.setColor(DIM);
        res.shapeRenderer.rect(0, 0, frame.screenW(), frame.screenH());
        res.shapeRenderer.end();

        res.spriteBatch.begin();

        String title = "YOU DIED";
        res.glyphLayout.setText(res.fontLarge, title);
        res.fontLarge.setColor(TITLE_COLOR);
        res.fontLarge.draw(res.spriteBatch, title,
                (frame.screenW() - res.glyphLayout.width) / 2f,
                frame.screenH() / 2f + res.glyphLayout.height);

        String hint = "SPACE to respawn    ESC to leave";
        res.glyphLayout.setText(res.fontMedium, hint);
        res.fontMedium.setColor(Color.WHITE);
        res.fontMedium.draw(res.spriteBatch, hint,
                (frame.screenW() - res.glyphLayout.width) / 2f,
                frame.screenH() / 2f - 12f);

        res.spriteBatch.end();
    }
}
