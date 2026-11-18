package ee.taltech.wiseones.geotank.clientcore.screen.theme;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Reusable themed background — a vertical gradient drawn before any
 * scene2d Stage renders.
 *
 * Each menu Screen owns one of these and calls {@link #render} from its
 * own {@code render(delta)}. ShapeRenderer state is fully managed inside,
 * so callers don't have to worry about leaving GL state dirty.
 */
public final class ScreenBackground {
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();

    /**
     * Clear the screen and paint the themed gradient. Should be called at the
     * very top of every menu Screen's render cycle, before stage.draw().
     */
    public void render() {
        Gdx.gl.glClearColor(ScreenTheme.BG_BOTTOM.r, ScreenTheme.BG_BOTTOM.g,
                ScreenTheme.BG_BOTTOM.b, ScreenTheme.BG_BOTTOM.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
        shapeRenderer.updateMatrices();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Bottom-left, bottom-right, top-right, top-left — the gradient fades from
        // BG_BOTTOM at the bottom to BG_TOP at the top, giving a subtle depth cue.
        shapeRenderer.rect(0, 0, w, h,
                ScreenTheme.BG_BOTTOM, ScreenTheme.BG_BOTTOM,
                ScreenTheme.BG_TOP, ScreenTheme.BG_TOP);
        shapeRenderer.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
