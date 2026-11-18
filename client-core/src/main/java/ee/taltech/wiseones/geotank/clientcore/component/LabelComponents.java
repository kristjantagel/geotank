package ee.taltech.wiseones.geotank.clientcore.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;

import static ee.taltech.wiseones.geotank.clientcore.util.Font.getArialFont;

/**
 * Utility class for creating different types of labels for the HUD.
 * Provides methods to generate standard labels or labels with a background.
 */
public class LabelComponents {
    private LabelComponents() {
    }

    /**
     * Creates a simple label with the specified text, color, and font size.
     */
    public static Label createLabel(String text, Color textColor, int fontSize) {
        BitmapFont font = getArialFont(fontSize);
        LabelStyle labelStyle = new LabelStyle(font, textColor);
        return new Label(text, labelStyle);
    }
}
