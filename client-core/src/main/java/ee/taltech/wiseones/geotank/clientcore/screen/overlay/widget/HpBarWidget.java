package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;

/**
 * Health bar above the XP bar.
 *
 * Width matches the XP bar so the player has a single, central read of
 * their state. Fill color interpolates green → amber → red as HP drops,
 * giving a quick visual urgency cue without needing to read the number.
 */
public final class HpBarWidget implements HudWidget {
    public static final float HEIGHT = 14f;
    public static final float GAP_ABOVE_XP = 4f;

    private static final Color BAR_BG = new Color(0.05f, 0.05f, 0.05f, 0.55f);
    private static final Color FILL_FULL = new Color(0.30f, 0.85f, 0.40f, 1f);
    private static final Color FILL_MID = new Color(1f, 0.78f, 0.20f, 1f);
    private static final Color FILL_LOW = new Color(1f, 0.30f, 0.30f, 1f);
    private static final Color OUTLINE = new Color(0.05f, 0.05f, 0.05f, 0.6f);

    private final HudResources res;

    public HpBarWidget(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean visible(HudFrame frame) {
        return frame.viewModel().localMaxLives() > 0;
    }

    /** Y-coordinate of the HP bar's bottom edge — directly above the XP bar. */
    public static float bottomY() {
        return XpBarWidget.BOTTOM_MARGIN + XpBarWidget.HEIGHT + GAP_ABOVE_XP;
    }

    public static float topY() {
        return bottomY() + HEIGHT;
    }

    @Override
    public void drawFilled(HudFrame frame) {
        float barW = frame.screenW() * XpBarWidget.WIDTH_RATIO;
        float barX = (frame.screenW() - barW) / 2f;
        float barY = bottomY();

        res.shapeRenderer.setColor(BAR_BG);
        res.shapeRenderer.rect(barX, barY, barW, HEIGHT);

        float ratio = (float) frame.viewModel().localLives() / frame.viewModel().localMaxLives();
        ratio = Math.max(0f, Math.min(1f, ratio));
        if (ratio > 0f) {
            res.shapeRenderer.setColor(fillColor(ratio));
            res.shapeRenderer.rect(barX, barY, barW * ratio, HEIGHT);
        }
    }

    @Override
    public void drawOutline(HudFrame frame) {
        float barW = frame.screenW() * XpBarWidget.WIDTH_RATIO;
        float barX = (frame.screenW() - barW) / 2f;
        res.shapeRenderer.setColor(OUTLINE);
        res.shapeRenderer.rect(barX, bottomY(), barW, HEIGHT);
    }

    @Override
    public void drawText(HudFrame frame) {
        String hpText = frame.viewModel().localLives() + " / " + frame.viewModel().localMaxLives();
        res.glyphLayout.setText(res.fontSmall, hpText);
        float textX = (frame.screenW() - res.glyphLayout.width) / 2f;
        float textY = HudUtil.centeredTextY(bottomY(), HEIGHT, res.glyphLayout.height);
        res.fontSmall.setColor(Color.WHITE);
        res.fontSmall.draw(res.spriteBatch, hpText, textX, textY);
    }

    /**
     * Above 60% HP fades from amber to green. Below 60% fades from red to amber.
     * Smooth blend avoids the abrupt color jumps that flat thresholds produce.
     */
    private static Color fillColor(float ratio) {
        if (ratio >= 0.6f) {
            float t = (ratio - 0.6f) / 0.4f;
            return HudUtil.lerpColor(FILL_MID, FILL_FULL, t);
        }
        float t = ratio / 0.6f;
        return HudUtil.lerpColor(FILL_LOW, FILL_MID, t);
    }
}
