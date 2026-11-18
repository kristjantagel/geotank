package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;

/**
 * A full-screen, conditional HUD layer (pause menu, death screen, class
 * selector modal). Renders independently of the regular widget passes —
 * the orchestrator calls {@link #render} after the always-on widgets so
 * overlays sit on top, and the implementation owns its batch begin/end.
 *
 * Hit-testing helpers (mouse-under-button, etc.) are added per overlay
 * where input handling needs them.
 */
public interface HudOverlay {
    boolean active(HudFrame frame);

    void render(HudFrame frame);
}
