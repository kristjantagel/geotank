package ee.taltech.wiseones.geotank.clientcore.component;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenTheme;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ThemedDrawables;
import ee.taltech.wiseones.geotank.clientcore.util.Font;

/**
 * Themed button factory used by every menu screen.
 *
 * Three variants form a clear hierarchy:
 * <ul>
 *   <li>{@link #getButton} — primary action (Start, Ready, Create). Filled
 *       in the accent color so it pulls the eye. One per screen ideally.</li>
 *   <li>{@link #getSecondaryButton} — alternate action (Settings, Cancel).
 *       Subtle dark fill that brightens on hover; no border at rest.</li>
 *   <li>{@link #getGhostButton} — tertiary action (Back, Exit). Text-only
 *       with a soft hover background. Recedes visually so it doesn't compete
 *       with the primary CTA.</li>
 * </ul>
 *
 * All variants share rounded corners and generous padding, and use a single
 * NinePatch background so the corners stay crisp at any size.
 */
public final class ButtonComponents {

    private ButtonComponents() {
    }

    /** Lambda-friendly click callback. */
    @FunctionalInterface
    public interface OnClickHandler {
        void handleClick();
    }

    /** Primary call-to-action button. Yellow fill, dark text, brightens on hover. */
    public static TextButton getButton(int fontSize, String text, OnClickHandler handler) {
        TextButtonStyle style = new TextButtonStyle();
        style.font = Font.getArialFont(fontSize);
        style.fontColor = ScreenTheme.ON_ACCENT;
        style.overFontColor = ScreenTheme.ON_ACCENT;
        style.downFontColor = ScreenTheme.ON_ACCENT;
        style.up = ThemedDrawables.roundedPanel(ScreenTheme.ACCENT, ScreenTheme.RADIUS_MEDIUM);
        style.over = ThemedDrawables.roundedPanel(ScreenTheme.ACCENT_HOVER, ScreenTheme.RADIUS_MEDIUM);
        style.down = ThemedDrawables.roundedPanel(ScreenTheme.ACCENT_PRESSED, ScreenTheme.RADIUS_MEDIUM);
        return buildButton(text, style, handler);
    }

    /**
     * Secondary action — filled dark surface, light text. Use when a screen
     * has more than one action and you want to deemphasize the alternates.
     */
    public static TextButton getSecondaryButton(int fontSize, String text, OnClickHandler handler) {
        TextButtonStyle style = new TextButtonStyle();
        style.font = Font.getArialFont(fontSize);
        style.fontColor = ScreenTheme.TEXT_PRIMARY;
        style.overFontColor = ScreenTheme.ACCENT;
        style.downFontColor = ScreenTheme.ACCENT;
        style.up = ThemedDrawables.roundedPanel(ScreenTheme.SURFACE, ScreenTheme.RADIUS_MEDIUM);
        style.over = ThemedDrawables.roundedPanel(ScreenTheme.SURFACE_HOVER, ScreenTheme.RADIUS_MEDIUM);
        style.down = ThemedDrawables.roundedPanel(ScreenTheme.SURFACE_HOVER, ScreenTheme.RADIUS_MEDIUM);
        return buildButton(text, style, handler);
    }

    /**
     * Tertiary / ghost action — text-only at rest, subtle wash on hover.
     * Used for Back, Cancel, Exit — recedes so it doesn't compete with the
     * primary call-to-action.
     */
    public static TextButton getGhostButton(int fontSize, String text, OnClickHandler handler) {
        TextButtonStyle style = new TextButtonStyle();
        style.font = Font.getArialFont(fontSize);
        style.fontColor = ScreenTheme.TEXT_SECONDARY;
        style.overFontColor = ScreenTheme.TEXT_PRIMARY;
        style.downFontColor = ScreenTheme.TEXT_PRIMARY;
        style.up = ThemedDrawables.transparent();
        style.over = ThemedDrawables.roundedPanel(ScreenTheme.SURFACE, ScreenTheme.RADIUS_MEDIUM);
        style.down = ThemedDrawables.roundedPanel(ScreenTheme.SURFACE_HOVER, ScreenTheme.RADIUS_MEDIUM);
        return buildButton(text, style, handler);
    }

    private static TextButton buildButton(String text, TextButtonStyle style, OnClickHandler handler) {
        TextButton button = new TextButton(text, style);
        button.pad(ScreenTheme.BUTTON_PAD_Y, ScreenTheme.BUTTON_PAD_X,
                ScreenTheme.BUTTON_PAD_Y, ScreenTheme.BUTTON_PAD_X);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handler.handleClick();
                super.clicked(event, x, y);
            }
        });
        return button;
    }
}
