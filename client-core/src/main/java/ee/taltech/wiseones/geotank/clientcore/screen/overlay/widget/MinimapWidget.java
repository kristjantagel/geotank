package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;
import ee.taltech.wiseones.geotank.clientcore.viewmodel.HudViewModel;
import ee.taltech.wiseones.geotank.shared.message.dto.DestructibleType;

import java.util.List;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.ARENA_UPPER_BOUND_X;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.ARENA_UPPER_BOUND_Y;

/**
 * Top-left minimap. 4:3 aspect to match the arena dimensions, so a tank's
 * dot position is a faithful linear scale of its world coords.
 *
 * The local player draws on top, slightly larger and in cyan, so it stands
 * out from enemies. Toggleable via the in-game "show minimap" setting.
 */
public final class MinimapWidget implements HudWidget {
    private static final float WIDTH = 160f;
    private static final float HEIGHT = 120f;
    private static final float LEFT_MARGIN = 8f;
    private static final float TOP_MARGIN = 8f;

    private static final Color PANEL_BG = new Color(0f, 0f, 0f, 0.55f);
    private static final Color BORDER = new Color(1f, 1f, 1f, 0.45f);
    private static final Color SHADOW = new Color(0f, 0f, 0f, 0.28f);

    private static final float TANK_RADIUS = 2.5f;
    private static final float LOCAL_TANK_RADIUS = 4f;
    private static final float DESTRUCTIBLE_RADIUS = 1.8f;

    private static final Color ENEMY = new Color(1f, 0.35f, 0.35f, 1f);
    private static final Color DEAD = new Color(0.45f, 0.45f, 0.45f, 0.8f);
    private static final Color LOCAL = new Color(0.26f, 0.8f, 1f, 1f);
    private static final Color BOX = new Color(1f, 0.87f, 0.26f, 1f);
    private static final Color SPHERE = new Color(1f, 0.35f, 0.35f, 1f);
    private static final Color HEXAGON = new Color(0.36f, 0.55f, 1f, 1f);

    private final HudResources res;

    public MinimapWidget(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean visible(HudFrame frame) {
        return frame.viewModel().showMinimap();
    }

    @Override
    public void drawFilled(HudFrame frame) {
        float x = LEFT_MARGIN;
        float y = frame.screenH() - TOP_MARGIN - HEIGHT;

        res.shapeRenderer.setColor(SHADOW);
        res.shapeRenderer.rect(x + 3f, y - 3f, WIDTH, HEIGHT);

        res.shapeRenderer.setColor(PANEL_BG);
        res.shapeRenderer.rect(x, y, WIDTH, HEIGHT);

        List<HudViewModel.MinimapDestructible> destructibles = frame.viewModel().minimapDestructibles();
        if (destructibles != null) {
            for (HudViewModel.MinimapDestructible d : destructibles) {
                float dx = x + d.x() / ARENA_UPPER_BOUND_X * WIDTH;
                float dy = y + d.y() / ARENA_UPPER_BOUND_Y * HEIGHT;
                res.shapeRenderer.setColor(destructibleColor(d.type()));
                res.shapeRenderer.circle(dx, dy, DESTRUCTIBLE_RADIUS);
            }
        }

        // Draw enemies first, local tank last so it sits on top.
        List<HudViewModel.MinimapTank> tanks = frame.viewModel().minimapTanks();
        if (tanks != null) {
            for (HudViewModel.MinimapTank t : tanks) {
                if (t.isLocal()) continue;
                float tx = x + t.x() / ARENA_UPPER_BOUND_X * WIDTH;
                float ty = y + t.y() / ARENA_UPPER_BOUND_Y * HEIGHT;
                res.shapeRenderer.setColor(t.alive() ? ENEMY : DEAD);
                res.shapeRenderer.circle(tx, ty, TANK_RADIUS);
            }
            for (HudViewModel.MinimapTank t : tanks) {
                if (!t.isLocal()) continue;
                float tx = x + t.x() / ARENA_UPPER_BOUND_X * WIDTH;
                float ty = y + t.y() / ARENA_UPPER_BOUND_Y * HEIGHT;
                res.shapeRenderer.setColor(LOCAL);
                res.shapeRenderer.circle(tx, ty, LOCAL_TANK_RADIUS);
            }
        }
    }

    @Override
    public void drawOutline(HudFrame frame) {
        float x = LEFT_MARGIN;
        float y = frame.screenH() - TOP_MARGIN - HEIGHT;
        res.shapeRenderer.setColor(BORDER);
        res.shapeRenderer.rect(x, y, WIDTH, HEIGHT);
    }

    private static Color destructibleColor(DestructibleType type) {
        if (type == null) return BOX;
        return switch (type) {
            case BOX -> BOX;
            case SPHERE -> SPHERE;
            case HEXAGON -> HEXAGON;
        };
    }
}
