package ee.taltech.wiseones.geotank.clientcore.component;

import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ScreenTheme;
import ee.taltech.wiseones.geotank.clientcore.screen.theme.ThemedDrawables;
import ee.taltech.wiseones.geotank.clientcore.util.Font;

/**
 * Themed TextField factory.
 *
 * Sunken dark surface so the field reads as recessed against the page. A
 * soft hairline border at rest brightens to the accent color when the
 * field has focus, giving an instant cue without animation. Both surfaces
 * are NinePatch-rounded so the corners stay sharp at any size.
 */
public final class TextFieldComponents {

    private static Drawable backgroundDrawable;
    private static Drawable focusedBackgroundDrawable;
    private static Drawable cursorDrawable;
    private static Drawable selectionDrawable;

    // Slightly transparent yellow highlight reads cleanly over the dark fill
    // without bleaching the text underneath.
    private static final com.badlogic.gdx.graphics.Color SELECTION_TINT =
            new com.badlogic.gdx.graphics.Color(1f, 0.88f, 0.32f, 0.35f);

    private TextFieldComponents() {
    }

    public static TextField createTextField(String placeholder, int width, int height, int maxLength, int fontSize) {
        ensureDrawables();
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = Font.getArialFont(fontSize);
        style.fontColor = ScreenTheme.TEXT_PRIMARY;
        style.disabledFontColor = ScreenTheme.TEXT_MUTED;
        style.messageFont = Font.getArialFont(fontSize);
        style.messageFontColor = ScreenTheme.TEXT_MUTED;
        style.background = backgroundDrawable;
        style.focusedBackground = focusedBackgroundDrawable;
        style.cursor = cursorDrawable;
        style.selection = selectionDrawable;

        TextField field = new TextField("", style);
        field.setMaxLength(maxLength);
        field.setMessageText(placeholder);
        field.setWidth(width);
        field.setHeight(height);
        return field;
    }

    /**
     * Build the singleton drawables once. We can't pre-build at class load
     * time because the LibGDX graphics context might not exist yet (e.g.
     * during static analysis or unit tests).
     */
    private static void ensureDrawables() {
        if (backgroundDrawable != null) return;

        backgroundDrawable = ThemedDrawables.roundedBordered(
                ScreenTheme.SUNKEN, ScreenTheme.BORDER, ScreenTheme.RADIUS_MEDIUM);
        focusedBackgroundDrawable = ThemedDrawables.roundedBordered(
                ScreenTheme.SUNKEN, ScreenTheme.ACCENT, ScreenTheme.RADIUS_MEDIUM);
        cursorDrawable = ThemedDrawables.solidFill(ScreenTheme.ACCENT);
        selectionDrawable = ThemedDrawables.solidFill(SELECTION_TINT);

        Drawable padded = backgroundDrawable;
        padded.setLeftWidth(ScreenTheme.FIELD_PAD_X);
        padded.setRightWidth(ScreenTheme.FIELD_PAD_X);
        padded.setTopHeight(ScreenTheme.FIELD_PAD_Y);
        padded.setBottomHeight(ScreenTheme.FIELD_PAD_Y);
        Drawable focusPadded = focusedBackgroundDrawable;
        focusPadded.setLeftWidth(ScreenTheme.FIELD_PAD_X);
        focusPadded.setRightWidth(ScreenTheme.FIELD_PAD_X);
        focusPadded.setTopHeight(ScreenTheme.FIELD_PAD_Y);
        focusPadded.setBottomHeight(ScreenTheme.FIELD_PAD_Y);
    }
}
