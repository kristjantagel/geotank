package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;
import ee.taltech.wiseones.geotank.clientcore.viewmodel.HudViewModel;

import java.util.List;

/**
 * Top-right scoreboard panel.
 * <p>
 * Each entry shows a name + score and a proportional fill bar relative to
 * the top scorer, so the user can read relative standings at a glance.
 * The local player's row is highlighted in cyan.
 */
public final class LeaderboardWidget implements HudWidget {
    private static final float WIDTH = 200f;
    private static final float ENTRY_HEIGHT = 22f;
    private static final float TITLE_HEIGHT = 28f;
    private static final float GAP = 2f;
    private static final float PAD_X = 8f;
    private static final float PANEL_PAD_BOTTOM = 8f;
    private static final float RIGHT_MARGIN = 8f;
    private static final float TOP_MARGIN = 8f;

    private static final Color FILL = new Color(0.26f, 1f, 0.57f, 1f);
    private static final Color FILL_LOCAL = new Color(0.26f, 0.8f, 1f, 1f);
    private static final Color PANEL_BG = new Color(0f, 0f, 0f, 0.55f);
    private static final Color HEADER_BG = new Color(0f, 0f, 0f, 0.72f);
    private static final Color ENTRY_BG = new Color(0.07f, 0.08f, 0.10f, 0.55f);
    private static final Color ACCENT = new Color(1f, 0.91f, 0.41f, 1f);
    private static final Color SHADOW = new Color(0f, 0f, 0f, 0.28f);
    private static final Color OTHER_NAME = new Color(0.94f, 0.94f, 0.94f, 1f);

    private final HudResources res;

    public LeaderboardWidget(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean visible(HudFrame frame) {
        List<HudViewModel.LeaderboardEntry> entries = frame.viewModel().leaderboard();
        return entries != null && !entries.isEmpty();
    }

    @Override
    public void drawFilled(HudFrame frame) {
        List<HudViewModel.LeaderboardEntry> entries = frame.viewModel().leaderboard();

        float x = frame.screenW() - WIDTH - RIGHT_MARGIN;
        float topY = frame.screenH() - TOP_MARGIN;
        float panelH = TITLE_HEIGHT
                + entries.size() * ENTRY_HEIGHT
                + Math.max(0, entries.size() - 1) * GAP
                + PANEL_PAD_BOTTOM;
        float panelY = topY - panelH;

        // Soft drop shadow
        res.shapeRenderer.setColor(SHADOW);
        res.shapeRenderer.rect(x + 3f, panelY - 3f, WIDTH, panelH);

        // Unified panel background
        res.shapeRenderer.setColor(PANEL_BG);
        res.shapeRenderer.rect(x, panelY, WIDTH, panelH);

        // Darker title strip + accent line
        float titleBottom = topY - TITLE_HEIGHT;
        res.shapeRenderer.setColor(HEADER_BG);
        res.shapeRenderer.rect(x, titleBottom, WIDTH, TITLE_HEIGHT);
        res.shapeRenderer.setColor(ACCENT);
        res.shapeRenderer.rect(x, titleBottom - 1f, WIDTH, 1.5f);

        int maxScore = entries.stream().mapToInt(HudViewModel.LeaderboardEntry::score).max().orElse(1);
        if (maxScore == 0) maxScore = 1;

        for (int i = 0; i < entries.size(); i++) {
            HudViewModel.LeaderboardEntry entry = entries.get(i);
            float entryY = titleBottom - (i + 1) * ENTRY_HEIGHT - i * GAP - 4f;

            res.shapeRenderer.setColor(ENTRY_BG);
            res.shapeRenderer.rect(x + 4f, entryY, WIDTH - 8f, ENTRY_HEIGHT);

            float fillRatio = (float) entry.score() / maxScore;
            Color color = entry.isLocal() ? FILL_LOCAL : FILL;
            res.shapeRenderer.setColor(color.r, color.g, color.b, 0.55f);
            res.shapeRenderer.rect(x + 4f, entryY, (WIDTH - 8f) * fillRatio, ENTRY_HEIGHT);
        }
    }

    @Override
    public void drawText(HudFrame frame) {
        List<HudViewModel.LeaderboardEntry> entries = frame.viewModel().leaderboard();

        float x = frame.screenW() - WIDTH - RIGHT_MARGIN;
        float topY = frame.screenH() - TOP_MARGIN;
        float titleBottom = topY - TITLE_HEIGHT;

        res.fontMedium.setColor(Color.WHITE);
        res.glyphLayout.setText(res.fontMedium, "Scoreboard");
        res.fontMedium.draw(res.spriteBatch, "Scoreboard",
                x + (WIDTH - res.glyphLayout.width) / 2f,
                HudUtil.centeredTextY(titleBottom, TITLE_HEIGHT, res.glyphLayout.height));

        for (int i = 0; i < entries.size(); i++) {
            HudViewModel.LeaderboardEntry entry = entries.get(i);
            float entryY = titleBottom - (i + 1) * ENTRY_HEIGHT - i * GAP - 4f;

            String name = entry.name();
            String scoreStr = HudUtil.formatScore(entry.score());

            // Name on the left (anchored), score right-aligned (no overlap).
            res.glyphLayout.setText(res.fontSmall, name);
            float nameH = res.glyphLayout.height;
            res.fontSmall.setColor(entry.isLocal() ? Color.WHITE : OTHER_NAME);
            res.fontSmall.draw(res.spriteBatch, name,
                    x + PAD_X,
                    HudUtil.centeredTextY(entryY, ENTRY_HEIGHT, nameH));

            res.glyphLayout.setText(res.fontSmall, scoreStr);
            res.fontSmall.setColor(Color.WHITE);
            res.fontSmall.draw(res.spriteBatch, scoreStr,
                    x + WIDTH - PAD_X - res.glyphLayout.width,
                    HudUtil.centeredTextY(entryY, ENTRY_HEIGHT, res.glyphLayout.height));
        }
    }
}
