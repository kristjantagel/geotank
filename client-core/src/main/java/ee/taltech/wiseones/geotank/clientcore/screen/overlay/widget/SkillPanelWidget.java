package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;
import ee.taltech.wiseones.geotank.shared.message.dto.SkillType;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.MAX_SKILL_STAT_LEVEL;

/**
 * Bottom-left compact skill upgrade panel (diep.io style).
 * <p>
 * One short bar per skill, each with up to {@code MAX_SKILL_STAT_LEVEL}
 * filled segments. Hidden until the player either has skill points to spend
 * or has invested any points — keeps the bottom-left corner clean for new
 * players who haven't unlocked anything yet.
 */
public final class SkillPanelWidget implements HudWidget {
    public static final float BAR_WIDTH = 150f;
    private static final float BAR_HEIGHT = 16f;
    private static final float GAP = 3f;
    public static final float LEFT_MARGIN = 8f;
    public static final float BOTTOM_MARGIN = 52f;

    private static final Color BAR_BG = new Color(0.05f, 0.05f, 0.05f, 0.55f);
    private static final Color HEADER = new Color(1f, 1f, 0.5f, 1f);
    private static final Color DIVIDER = new Color(0.12f, 0.12f, 0.12f, 0.75f);

    // Diep.io stat colors, ordered by SkillType ordinal.
    private static final Color[] STAT_COLORS = {
            new Color(0.99f, 0.68f, 0.46f, 1f),  // Health Regen (peach)
            new Color(0.98f, 0.26f, 1f, 1f),     // Max Health (magenta)
            new Color(0.78f, 0.56f, 1f, 1f),     // Body Damage (light purple)
            new Color(0.26f, 0.50f, 1f, 1f),     // Bullet Speed (blue)
            new Color(1f, 0.87f, 0.26f, 1f),     // Bullet Penetration (yellow)
            new Color(1f, 0.26f, 0.26f, 1f),     // Bullet Damage (red)
            new Color(0.51f, 1f, 0.26f, 1f),     // Reload (green)
            new Color(0.26f, 1f, 0.98f, 1f),     // Movement Speed (cyan)
    };

    // Indexed by SkillType ordinal.
    private static final String[] STAT_NAMES = {
            "Health Regen", "Max Health", "Body Damage", "Bullet Speed",
            "Pierce", "Bullet Damage", "Fire Rate", "Movement Speed"
    };

    private final HudResources res;

    public SkillPanelWidget(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean visible(HudFrame frame) {
        return shouldShow(frame.viewModel().skillLevels(), frame.viewModel().localSkillPoints());
    }

    @Override
    public void drawFilled(HudFrame frame) {
        int[] levels = frame.viewModel().skillLevels();
        SkillType[] types = SkillType.values();
        for (int i = 0; i < types.length; i++) {
            float barX = LEFT_MARGIN;
            float barY = BOTTOM_MARGIN + (types.length - 1 - i) * (BAR_HEIGHT + GAP);

            res.shapeRenderer.setColor(BAR_BG);
            res.shapeRenderer.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT);

            int statLevel = (levels != null && i < levels.length) ? levels[i] : 0;
            if (statLevel > 0) {
                float segmentWidth = BAR_WIDTH / MAX_SKILL_STAT_LEVEL;
                res.shapeRenderer.setColor(STAT_COLORS[i]);
                res.shapeRenderer.rect(barX, barY, segmentWidth * statLevel, BAR_HEIGHT);
            }

            res.shapeRenderer.setColor(DIVIDER);
            float segW = BAR_WIDTH / MAX_SKILL_STAT_LEVEL;
            for (int s = 1; s < MAX_SKILL_STAT_LEVEL; s++) {
                res.shapeRenderer.rect(barX + segW * s - 0.5f, barY, 1f, BAR_HEIGHT);
            }
        }
    }

    @Override
    public void drawText(HudFrame frame) {
        int[] levels = frame.viewModel().skillLevels();

        if (frame.viewModel().localSkillPoints() > 0) {
            int sp = frame.viewModel().localSkillPoints();
            String header = sp + " skill point" + (sp > 1 ? "s" : "") + " available (1-8)";
            res.fontSmall.setColor(HEADER);
            float headerY = BOTTOM_MARGIN + SkillType.values().length * (BAR_HEIGHT + GAP) + 4f;
            res.fontSmall.draw(res.spriteBatch, header, LEFT_MARGIN, headerY + 10f);
        }

        SkillType[] types = SkillType.values();
        for (int i = 0; i < types.length; i++) {
            float barY = BOTTOM_MARGIN + (types.length - 1 - i) * (BAR_HEIGHT + GAP);
            int statLevel = (levels != null && i < levels.length) ? levels[i] : 0;
            String label = (i + 1) + " " + STAT_NAMES[i] + " " + statLevel + "/" + MAX_SKILL_STAT_LEVEL;
            res.fontSmall.setColor(STAT_COLORS[i]);
            res.fontSmall.draw(res.spriteBatch, label, LEFT_MARGIN + 3f, barY + BAR_HEIGHT - 2f);
        }
    }

    /** Show the panel when the player has points to spend or has invested any. */
    private static boolean shouldShow(int[] levels, int skillPoints) {
        if (skillPoints > 0) return true;
        if (levels != null) {
            for (int lvl : levels) {
                if (lvl > 0) return true;
            }
        }
        return false;
    }
}
