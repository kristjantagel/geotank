package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;

/**
 * Small pure helpers used across multiple HUD widgets. Anything that grew
 * beyond two callers and didn't belong to a specific widget moved here.
 */
public final class HudUtil {
    private HudUtil() {
    }

    /**
     * Y-coordinate (top-of-glyph in libgdx convention) for vertically centering
     * text inside a box of {@code boxHeight} starting at {@code boxBottom}.
     */
    public static float centeredTextY(float boxBottom, float boxHeight, float textHeight) {
        return boxBottom + (boxHeight + textHeight) / 2f;
    }

    /** Linear interpolation between two colors, clamped to [0,1]. */
    public static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                a.r + (b.r - a.r) * t,
                a.g + (b.g - a.g) * t,
                a.b + (b.b - a.b) * t,
                a.a + (b.a - a.a) * t);
    }

    /** Compact score display: 1234 → "1.2k". Below 1000 stays as-is. */
    public static String formatScore(int score) {
        if (score >= 1000) {
            return String.format("%.1fk", score / 1000f);
        }
        return String.valueOf(score);
    }

    /**
     * Format a player's class label for the XP bar.
     * Returns "Tank" before any selection (matches the pre-class-system label),
     * the class name for a single-class build, and "A + B" for multiclass.
     */
    public static String classDisplay(TankClass primary, TankClass secondary) {
        if (primary == null || primary == TankClass.DEFAULT) {
            return "Tank";
        }
        if (secondary == null || secondary == TankClass.DEFAULT) {
            return primary.getDisplayName();
        }
        return primary.getDisplayName() + " + " + secondary.getDisplayName();
    }
}
