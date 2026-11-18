package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;

/**
 * A persistent, always-on HUD element (XP bar, leaderboard, minimap, etc.).
 *
 * The orchestrator drives three render passes — filled shapes, line shapes,
 * and text — and calls into each widget for whichever passes it needs.
 * Default no-op implementations let a widget participate in only the passes
 * it cares about (e.g. text-only widgets like FPS counter).
 *
 * Conditional full-screen overlays (pause menu, death screen, class selector)
 * implement {@link HudOverlay} instead so they can manage their own batch state.
 */
public interface HudWidget {
    /** Whether the widget should render this frame. */
    default boolean visible(HudFrame frame) {
        return true;
    }

    /** Filled-shape pass — runs inside a single {@code shapeRenderer.begin(Filled)}. */
    default void drawFilled(HudFrame frame) {
    }

    /** Line-shape (outline) pass — runs inside a single {@code shapeRenderer.begin(Line)}. */
    default void drawOutline(HudFrame frame) {
    }

    /** Text pass — runs inside a single {@code spriteBatch.begin()}. */
    default void drawText(HudFrame frame) {
    }
}
