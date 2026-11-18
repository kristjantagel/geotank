package ee.taltech.wiseones.geotank.clientcore.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized font factory. Each generated {@link BitmapFont} owns a
 * GPU-resident glyph atlas, so we cache by (face, size) — without this,
 * every Label creation would upload a fresh atlas, and rebuilds (like a
 * lobby roster on each {@code LobbyStateMessage}) would leak GPU memory
 * indefinitely.
 *
 * Cached fonts are shared across many Labels; scene2d's LabelStyle copies
 * the reference, so reuse is safe.
 */
public final class Font {
    private static final FreeTypeFontGenerator pixelFontGenerator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/PixelifySans-VariableFont_wght.ttf"));
    private static final FreeTypeFontGenerator arialFontGenerator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/Arimo-VariableFont_wght.ttf"));

    private static final Map<Integer, BitmapFont> pixelCache = new HashMap<>();
    private static final Map<Integer, BitmapFont> arialCache = new HashMap<>();

    private Font() {
    }

    public static BitmapFont getPixelFont(int fontSize) {
        return pixelCache.computeIfAbsent(fontSize, s -> generate(pixelFontGenerator, s));
    }

    public static BitmapFont getArialFont(int fontSize) {
        return arialCache.computeIfAbsent(fontSize, s -> generate(arialFontGenerator, s));
    }

    private static BitmapFont generate(FreeTypeFontGenerator generator, int fontSize) {
        var parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = fontSize;
        // Linear filtering on the glyph atlas smooths font edges at non-native sizes
        // (e.g. when ExtendViewport scales the HUD on larger windows).
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        return generator.generateFont(parameter);
    }
}
