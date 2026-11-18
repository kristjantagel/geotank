package ee.taltech.wiseones.geotank.clientcore.screen.theme;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.HashMap;
import java.util.Map;

/**
 * Procedural themed drawable factory — every surface in the menus is a
 * NinePatch generated at runtime so we don't need any image assets and the
 * corners stay crisp at any size.
 * <p>
 * Why NinePatch (and not just stretching a tiny region):
 * <ul>
 *   <li>Stretching a small region with a baked-in border smears that border
 *       across the whole element — the cause of the murky shading on the
 *       previous iteration. NinePatch carves out the corners and only
 *       stretches the center, so the border stays a true 1-pixel line.</li>
 *   <li>Round corners stay round at any aspect ratio.</li>
 * </ul>
 * <p>
 * Antialiasing is computed per-pixel via a smoothstep over the signed
 * distance to the nearest corner — gives clean, soft edges without aliasing
 * stairsteps when the corner radius is small.
 * <p>
 * Drawables are cached by their visual signature (color + radius + border).
 * This is the difference between a leak and a sane runtime: callers like
 * {@code LobbyScreen.rebuildPlayerList} run every frame and would otherwise
 * upload a fresh GPU texture per row per frame.
 */
public final class ThemedDrawables {

    // Keyed by a signature string built from color/radius/border so reused
    // visuals don't re-upload textures. Drawables themselves are reusable
    // across many actors — scene2d copies the region on draw.
    private static final Map<String, Drawable> CACHE = new HashMap<>();

    private ThemedDrawables() {
    }

    // -------------------- Public factories --------------------

    /** Solid rounded panel with no border. Use for buttons + section panels. */
    public static Drawable roundedPanel(Color fill, int radius) {
        return CACHE.computeIfAbsent(
                "panel:" + signature(fill) + ":r" + radius,
                k -> ninePatchDrawable(roundedTexture(radius, fill, null, 0), radius));
    }

    /** Rounded panel with a 1 px border in {@code border}. */
    public static Drawable roundedBordered(Color fill, Color border, int radius) {
        return CACHE.computeIfAbsent(
                "border:" + signature(fill) + ":" + signature(border) + ":r" + radius,
                k -> ninePatchDrawable(roundedTexture(radius, fill, border, 1), radius));
    }

    /** Solid 1×1 fill — useful when you want a plain block (e.g. for cursors). */
    public static Drawable solidFill(Color color) {
        return CACHE.computeIfAbsent("solid:" + signature(color), k -> {
            Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            p.setColor(color);
            p.fill();
            Texture tex = new Texture(p);
            p.dispose();
            return new TextureRegionDrawable(new TextureRegion(tex));
        });
    }

    /** Transparent 1×1 — handy as a "no background" drawable. */
    public static Drawable transparent() {
        return solidFill(new Color(0f, 0f, 0f, 0f));
    }

    /** Color → stable string key. {@link Color#toString()} would also work but is intent-opaque. */
    private static String signature(Color c) {
        return Integer.toHexString(Color.rgba8888(c));
    }

    // -------------------- Texture rendering --------------------

    private static NinePatchDrawable ninePatchDrawable(Texture tex, int border) {
        NinePatch patch = new NinePatch(new TextureRegion(tex), border, border, border, border);
        return new NinePatchDrawable(patch);
    }

    /**
     * Render a rounded rect into a fresh pixmap. The pixmap is exactly
     * {@code 2*radius + 2} on each side — enough to fit four corners with a
     * 2 px stretchable middle that NinePatch will repeat across any size.
     * <p>
     * Antialiasing: for each pixel, compute distance to the corner the pixel
     * lives in (or 0 if on a straight edge). Smoothstep around the radius
     * gives a clean 1 px-wide alpha falloff.
     */
    private static Texture roundedTexture(int radius, Color fill, Color border, int borderPx) {
        int size = radius * 2 + 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);

        // Pre-extract channel ints to avoid repeated unpacking inside the loop.
        float fr = fill.r, fg = fill.g, fb = fill.b, fa = fill.a;
        boolean haveBorder = border != null && borderPx > 0;
        float br = haveBorder ? border.r : 0f;
        float bg = haveBorder ? border.g : 0f;
        float bb = haveBorder ? border.b : 0f;
        float ba = haveBorder ? border.a : 0f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Distance from the inner edge of the corner — 0 inside a straight
                // edge, increasing toward the corner's "outside".
                float dx = cornerOffset(x, radius, size);
                float dy = cornerOffset(y, radius, size);
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                // Outer-edge alpha falloff: 1 inside, 0 outside, ~1 px AA band on the edge.
                float outerAlpha = 1f - smoothstep(radius - 0.5f, radius + 0.5f, dist);
                if (outerAlpha <= 0f) {
                    p.drawPixel(x, y, 0);
                    continue;
                }

                float r, g, b, a;
                if (haveBorder) {
                    // Border->fill blend: 1 in the border ring (close to the edge), 0 inside.
                    float borderInner = radius - borderPx;
                    float borderRatio = smoothstep(borderInner - 0.5f, borderInner + 0.5f, dist);
                    r = lerp(fr, br, borderRatio);
                    g = lerp(fg, bg, borderRatio);
                    b = lerp(fb, bb, borderRatio);
                    a = outerAlpha * lerp(fa, ba, borderRatio);
                } else {
                    r = fr;
                    g = fg;
                    b = fb;
                    a = fa * outerAlpha;
                }

                p.drawPixel(x, y, packRgba(r, g, b, a));
            }
        }

        Texture tex = new Texture(p);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return tex;
    }

    // -------------------- Math helpers --------------------

    /**
     * For a coordinate inside a {@code size}-wide axis with {@code radius}
     * corner space, return the distance from the corner's inner edge.
     * 0 along straight edges, increases toward the corner's outside.
     */
    private static float cornerOffset(int coord, int radius, int size) {
        if (coord < radius) return radius - coord;
        if (coord >= size - radius) return coord - (size - radius - 1);
        return 0f;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        if (x <= edge0) return 0f;
        if (x >= edge1) return 1f;
        float t = (x - edge0) / (edge1 - edge0);
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int packRgba(float r, float g, float b, float a) {
        int ri = clamp255((int) (r * 255f));
        int gi = clamp255((int) (g * 255f));
        int bi = clamp255((int) (b * 255f));
        int ai = clamp255((int) (a * 255f));
        return (ri << 24) | (gi << 16) | (bi << 8) | ai;
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
