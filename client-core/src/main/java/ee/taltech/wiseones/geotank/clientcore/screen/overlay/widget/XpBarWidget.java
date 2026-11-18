package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;

/**
 * Bottom-center XP bar with the level + class label above it and the
 * compact score below. Width and Y constants are public so {@link HpBarWidget}
 * can stack on top without hardcoding magic numbers.
 */
public final class XpBarWidget implements HudWidget {
    public static final float WIDTH_RATIO = 0.50f;
    public static final float HEIGHT = 22f;
    public static final float BOTTOM_MARGIN = 18f;

    private static final Color BAR_BG = new Color(0.05f, 0.05f, 0.05f, 0.55f);
    private static final Color FILL = new Color(1f, 0.91f, 0.41f, 1f);
    private static final Color OUTLINE = new Color(0.7f, 0.63f, 0.2f, 0.8f);
    private static final Color SCORE_TEXT = new Color(0.85f, 0.85f, 0.85f, 0.85f);

    private final HudResources res;

    public XpBarWidget(HudResources res) {
        this.res = res;
    }

    @Override
    public void drawFilled(HudFrame frame) {
        float barW = frame.screenW() * WIDTH_RATIO;
        float barX = (frame.screenW() - barW) / 2f;

        res.shapeRenderer.setColor(BAR_BG);
        res.shapeRenderer.rect(barX, BOTTOM_MARGIN, barW, HEIGHT);

        // Progress is XP gained within the current level, not cumulative XP.
        int xpInLevel = frame.viewModel().localXp() - frame.viewModel().xpForCurrentLevel();
        int xpNeeded = frame.viewModel().xpForNextLevel() - frame.viewModel().xpForCurrentLevel();
        if (xpNeeded > 0) {
            float progress = Math.min(1f, (float) xpInLevel / xpNeeded);
            if (progress > 0) {
                res.shapeRenderer.setColor(FILL);
                res.shapeRenderer.rect(barX, BOTTOM_MARGIN, barW * progress, HEIGHT);
            }
        }
    }

    @Override
    public void drawOutline(HudFrame frame) {
        float barW = frame.screenW() * WIDTH_RATIO;
        float barX = (frame.screenW() - barW) / 2f;
        res.shapeRenderer.setColor(OUTLINE);
        res.shapeRenderer.rect(barX, BOTTOM_MARGIN, barW, HEIGHT);
    }

    @Override
    public void drawText(HudFrame frame) {
        // Level + class label centered above the HP bar (which sits above the XP bar).
        String levelText = "Level " + frame.viewModel().localLevel() + " "
                + HudUtil.classDisplay(frame.viewModel().localPrimaryClass(),
                                       frame.viewModel().localSecondaryClass());
        res.glyphLayout.setText(res.fontMedium, levelText);
        float textX = (frame.screenW() - res.glyphLayout.width) / 2f;
        float textY = HpBarWidget.topY() + res.glyphLayout.height + 4f;
        res.fontMedium.setColor(Color.WHITE);
        res.fontMedium.draw(res.spriteBatch, levelText, textX, textY);

        // Score below the XP bar. BitmapFont.draw treats y as the TOP of the text,
        // so 4 px below the bar's bottom edge places the label cleanly beneath.
        String scoreText = "Score: " + HudUtil.formatScore(frame.viewModel().localScore());
        res.glyphLayout.setText(res.fontSmall, scoreText);
        float scoreX = (frame.screenW() - res.glyphLayout.width) / 2f;
        float scoreY = BOTTOM_MARGIN - 4f;
        res.fontSmall.setColor(SCORE_TEXT);
        res.fontSmall.draw(res.spriteBatch, scoreText, scoreX, scoreY);
    }
}
