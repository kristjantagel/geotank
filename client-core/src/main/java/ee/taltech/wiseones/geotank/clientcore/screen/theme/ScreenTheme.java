package ee.taltech.wiseones.geotank.clientcore.screen.theme;

import com.badlogic.gdx.graphics.Color;

/**
 * Single source of truth for menu screen colors, shapes, sizing, and
 * typography.
 * <p>
 * The in-game HUD uses a dark/neon palette (yellow XP bar, cyan
 * highlights). The menus pick up the same vocabulary so the player isn't
 * jarred when transitioning to gameplay — same accent yellow, same dark
 * surfaces, same cyan call-outs.
 * <p>
 * Sizes assume a 1280×720 design canvas (matches {@code VIEWPORT_WIDTH/HEIGHT}).
 * Menu screens use a FitViewport at that size so the UI scales uniformly to
 * fill the actual window.
 */
public final class ScreenTheme {

    // -------------------- Background gradient --------------------
    public static final Color BG_TOP = new Color(0.04f, 0.05f, 0.07f, 1f);
    public static final Color BG_BOTTOM = new Color(0.09f, 0.10f, 0.14f, 1f);

    // -------------------- Surfaces --------------------
    public static final Color SURFACE = new Color(0.13f, 0.15f, 0.20f, 0.94f);
    public static final Color SURFACE_HOVER = new Color(0.19f, 0.22f, 0.28f, 1f);
    public static final Color SUNKEN = new Color(0.07f, 0.08f, 0.11f, 0.96f);
    public static final Color BORDER = new Color(0.32f, 0.36f, 0.44f, 0.55f);

    // -------------------- Accent --------------------
    public static final Color ACCENT = new Color(1f, 0.88f, 0.32f, 1f);
    public static final Color ACCENT_HOVER = new Color(1f, 0.95f, 0.55f, 1f);
    public static final Color ACCENT_PRESSED = new Color(0.88f, 0.76f, 0.20f, 1f);
    public static final Color ON_ACCENT = new Color(0.06f, 0.07f, 0.10f, 1f);
    public static final Color SECONDARY = new Color(0.34f, 0.74f, 1f, 1f);

    // -------------------- Status --------------------
    public static final Color SUCCESS = new Color(0.40f, 0.92f, 0.55f, 1f);
    public static final Color ERROR = new Color(1f, 0.42f, 0.42f, 1f);

    // -------------------- Text --------------------
    public static final Color TEXT_PRIMARY = new Color(0.96f, 0.97f, 1f, 1f);
    public static final Color TEXT_SECONDARY = new Color(0.74f, 0.78f, 0.86f, 1f);
    public static final Color TEXT_MUTED = new Color(0.50f, 0.55f, 0.64f, 1f);

    // -------------------- Shapes (corner radii) --------------------
    public static final int RADIUS_SMALL = 8;
    public static final int RADIUS_MEDIUM = 12;
    public static final int RADIUS_LARGE = 18;

    // -------------------- Spacing --------------------
    // Padding sizes target a 1280×720 canvas so they read generously.
    public static final int BUTTON_PAD_X = 36;
    public static final int BUTTON_PAD_Y = 18;
    public static final int FIELD_PAD_X = 18;
    public static final int FIELD_PAD_Y = 14;

    // -------------------- Typography --------------------
    // Sized to read clearly at the 1280×720 design canvas.
    public static final int FONT_HERO = 88;
    public static final int FONT_TITLE = 44;
    public static final int FONT_SUBTITLE = 20;
    public static final int FONT_BUTTON = 22;
    public static final int FONT_BUTTON_SMALL = 18;
    public static final int FONT_BODY = 18;
    public static final int FONT_LABEL = 14;
    public static final int FONT_CAPTION = 13;
    public static final int FONT_BADGE = 12;

    private ScreenTheme() {
    }
}
