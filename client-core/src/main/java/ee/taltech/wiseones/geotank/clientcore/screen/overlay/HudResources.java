package ee.taltech.wiseones.geotank.clientcore.screen.overlay;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * GPU resources shared by every HUD widget. Owned and disposed by {@link Hud}.
 *
 * Widgets receive this through their constructor and never create their own
 * fonts or batches — keeps disposal centralized and avoids fragmenting
 * resource ownership across a dozen tiny classes.
 */
public final class HudResources {
    public final SpriteBatch spriteBatch;
    public final ShapeRenderer shapeRenderer;
    public final BitmapFont fontSmall;
    public final BitmapFont fontMedium;
    public final BitmapFont fontLarge;
    public final GlyphLayout glyphLayout;

    public HudResources(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer,
                        BitmapFont fontSmall, BitmapFont fontMedium, BitmapFont fontLarge,
                        GlyphLayout glyphLayout) {
        this.spriteBatch = spriteBatch;
        this.shapeRenderer = shapeRenderer;
        this.fontSmall = fontSmall;
        this.fontMedium = fontMedium;
        this.fontLarge = fontLarge;
        this.glyphLayout = glyphLayout;
    }
}
