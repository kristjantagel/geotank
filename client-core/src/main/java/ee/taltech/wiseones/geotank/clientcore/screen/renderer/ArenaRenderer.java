package ee.taltech.wiseones.geotank.clientcore.screen.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ee.taltech.wiseones.geotank.clientcore.model.ArenaModel;
import ee.taltech.wiseones.geotank.clientcore.model.DestructibleModel;
import ee.taltech.wiseones.geotank.shared.util.Logger;
import ee.taltech.wiseones.geotank.clientcore.model.BulletModel;
import ee.taltech.wiseones.geotank.clientcore.model.TankModel;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

import java.util.List;
import java.util.Optional;

/**
 * Renders the game world with a diep.io visual style:
 * - Light gray grid background with subtle lines
 * - Dark boundary outside the playable arena
 * - Outlined shapes (destructibles, bullets)
 * - All rendering uses the game camera's projection matrix
 */
public class ArenaRenderer {
    private final RendererRegistry registry;
    private final ShapeRenderer shapeRenderer;

    // Diep.io-style grid
    private static final Color GRID_BG = new Color(0.804f, 0.804f, 0.804f, 1f);   // #CDCDCD
    private static final Color GRID_LINE = new Color(0.753f, 0.753f, 0.753f, 1f);  // #C0C0C0
    private static final Color BOUNDARY_COLOR = new Color(0.55f, 0.55f, 0.55f, 1f); // darker gray outside arena
    private static final float GRID_SPACING = 24f;

    // Diep.io destructible colors (fill + darker outline)
    private static final Color BOX_FILL = new Color(1f, 0.87f, 0.34f, 1f);         // yellow (#FFDE57)
    private static final Color BOX_OUTLINE = new Color(0.72f, 0.62f, 0.20f, 1f);
    private static final Color SPHERE_FILL = new Color(1f, 0.47f, 0.47f, 1f);      // red-pink (#FF7878)
    private static final Color SPHERE_OUTLINE = new Color(0.72f, 0.30f, 0.30f, 1f);
    private static final Color HEXAGON_FILL = new Color(0.47f, 0.47f, 1f, 1f);     // blue (#7878FF)
    private static final Color HEXAGON_OUTLINE = new Color(0.30f, 0.30f, 0.72f, 1f);

    // Bullet
    private static final Color BULLET_FILL = new Color(0.0f, 0.72f, 0.92f, 1f);    // blue like tank
    private static final Color BULLET_OUTLINE = new Color(0.0f, 0.50f, 0.65f, 1f);
    private static final float OUTLINE_W = 2f; // outline thickness for shapes

    public ArenaRenderer(RendererRegistry registry) {
        this.registry = registry;
        this.shapeRenderer = new ShapeRenderer();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    /**
     * Renders the full arena using the given camera for world-space projection.
     */
    public void render(ArenaModel model, SpriteBatch batch, OrthographicCamera camera) {
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        renderBoundary(camera);
        renderGrid(camera);
        renderDestructibles(model.getDestructibles());
        renderBullets(model.getBullets());
        renderTanks(model.getTanks(), batch);
    }

    /**
     * Draws a dark zone outside the arena bounds to clearly show the boundary.
     */
    private void renderBoundary(OrthographicCamera camera) {
        float camX = camera.position.x;
        float camY = camera.position.y;
        float halfW = camera.viewportWidth / 2;
        float halfH = camera.viewportHeight / 2;

        float viewLeft = camX - halfW;
        float viewRight = camX + halfW;
        float viewBottom = camY - halfH;
        float viewTop = camY + halfH;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BOUNDARY_COLOR);

        // Left boundary
        if (viewLeft < ARENA_LOWER_BOUND_X) {
            shapeRenderer.rect(viewLeft, viewBottom, ARENA_LOWER_BOUND_X - viewLeft, viewTop - viewBottom);
        }
        // Right boundary
        if (viewRight > ARENA_UPPER_BOUND_X) {
            shapeRenderer.rect(ARENA_UPPER_BOUND_X, viewBottom, viewRight - ARENA_UPPER_BOUND_X, viewTop - viewBottom);
        }
        // Bottom boundary
        if (viewBottom < ARENA_LOWER_BOUND_Y) {
            shapeRenderer.rect(viewLeft, viewBottom, viewRight - viewLeft, ARENA_LOWER_BOUND_Y - viewBottom);
        }
        // Top boundary
        if (viewTop > ARENA_UPPER_BOUND_Y) {
            shapeRenderer.rect(viewLeft, ARENA_UPPER_BOUND_Y, viewRight - viewLeft, viewTop - ARENA_UPPER_BOUND_Y);
        }

        shapeRenderer.end();
    }

    /**
     * Draws a diep.io-style grid background. Only draws lines visible in the current viewport.
     */
    private void renderGrid(OrthographicCamera camera) {
        float camX = camera.position.x;
        float camY = camera.position.y;
        float halfW = camera.viewportWidth / 2;
        float halfH = camera.viewportHeight / 2;

        // Visible area (clamped to arena)
        float left = Math.max(ARENA_LOWER_BOUND_X, camX - halfW);
        float right = Math.min(ARENA_UPPER_BOUND_X, camX + halfW);
        float bottom = Math.max(ARENA_LOWER_BOUND_Y, camY - halfH);
        float top = Math.min(ARENA_UPPER_BOUND_Y, camY + halfH);

        // Background fill (only inside arena)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(GRID_BG);
        shapeRenderer.rect(left, bottom, right - left, top - bottom);
        shapeRenderer.end();

        // Grid lines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(GRID_LINE);

        float startX = (float) (Math.floor(left / GRID_SPACING) * GRID_SPACING);
        for (float x = startX; x <= right; x += GRID_SPACING) {
            if (x >= ARENA_LOWER_BOUND_X && x <= ARENA_UPPER_BOUND_X) {
                shapeRenderer.line(x, bottom, x, top);
            }
        }
        float startY = (float) (Math.floor(bottom / GRID_SPACING) * GRID_SPACING);
        for (float y = startY; y <= top; y += GRID_SPACING) {
            if (y >= ARENA_LOWER_BOUND_Y && y <= ARENA_UPPER_BOUND_Y) {
                shapeRenderer.line(left, y, right, y);
            }
        }

        shapeRenderer.end();
    }

    private void renderTanks(List<TankModel> tanks, SpriteBatch batch) {
        for (TankModel tank : tanks) {
            if (!tank.isAlive()) continue;
            Logger.debug(tank.toString());
            Optional<TankRenderStrategy> strategy = registry.getTankStrategy(tank);
            strategy.ifPresent(tankRenderStrategy -> tankRenderStrategy.render(tank, batch, shapeRenderer));
        }
    }

    /**
     * Renders bullets as diep.io-style outlined circles instead of sprite textures.
     */
    private void renderBullets(List<BulletModel> bullets) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (BulletModel bullet : bullets) {
            float bx = bullet.getX();
            float by = bullet.getY();
            float r = BULLET_WIDTH_IN_PIXELS * HALF_FLOAT;

            // Outline circle (slightly larger)
            shapeRenderer.setColor(BULLET_OUTLINE);
            shapeRenderer.circle(bx, by, r + OUTLINE_W, CIRCLE_SEGMENTS);
            // Fill circle
            shapeRenderer.setColor(BULLET_FILL);
            shapeRenderer.circle(bx, by, r, CIRCLE_SEGMENTS);
        }
        shapeRenderer.end();
    }

    /**
     * Renders destructibles with diep.io-style fills and darker outlines.
     * Each type uses a distinct color: yellow squares, red-pink circles, blue hexagons.
     */
    private void renderDestructibles(List<DestructibleModel> destructibles) {
        // Pass 1: outlines (slightly larger shapes in darker color)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (DestructibleModel destructible : destructibles) {
            switch (destructible.getType()) {
                case BOX:
                    shapeRenderer.setColor(BOX_OUTLINE);
                    shapeRenderer.rect(
                            destructible.getX() - OUTLINE_W, destructible.getY() - OUTLINE_W,
                            BOX_WIDTH + OUTLINE_W * 2, BOX_HEIGHT + OUTLINE_W * 2);
                    break;
                case SPHERE:
                    shapeRenderer.setColor(SPHERE_OUTLINE);
                    shapeRenderer.circle(
                            destructible.getX() + SPHERE_WIDTH * HALF_FLOAT,
                            destructible.getY() + SPHERE_HEIGHT * HALF_FLOAT,
                            SPHERE_RADIUS + OUTLINE_W, CIRCLE_SEGMENTS);
                    break;
                case HEXAGON:
                    renderHexagonOutline(shapeRenderer, destructible);
                    break;
            }
        }
        shapeRenderer.end();

        // Pass 2: fills (normal-sized shapes on top)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (DestructibleModel destructible : destructibles) {
            switch (destructible.getType()) {
                case BOX:
                    shapeRenderer.setColor(BOX_FILL);
                    shapeRenderer.rect(destructible.getX(), destructible.getY(), BOX_WIDTH, BOX_HEIGHT);
                    break;
                case SPHERE:
                    shapeRenderer.setColor(SPHERE_FILL);
                    shapeRenderer.circle(
                            destructible.getX() + SPHERE_WIDTH * HALF_FLOAT,
                            destructible.getY() + SPHERE_HEIGHT * HALF_FLOAT,
                            SPHERE_RADIUS, CIRCLE_SEGMENTS);
                    break;
                case HEXAGON:
                    renderHexagonFill(shapeRenderer, destructible);
                    break;
            }
        }
        shapeRenderer.end();
    }

    /** Renders the hexagon outline for a destructible. */
    private void renderHexagonOutline(ShapeRenderer sr, DestructibleModel destructible) {
        drawHexagon(sr, destructible, HEXAGON_OUTLINE, OUTLINE_W);
    }

    /** Renders the hexagon fill for a destructible. */
    private void renderHexagonFill(ShapeRenderer sr, DestructibleModel destructible) {
        drawHexagon(sr, destructible, HEXAGON_FILL, 0);
    }

    /** Draws a hexagon centered in the destructible's bounds, optionally enlarged for outlines. */
    private void drawHexagon(ShapeRenderer sr, DestructibleModel destructible, Color color, float extra) {
        sr.setColor(color);
        float cx = destructible.getX() + HEXAGON_WIDTH * HALF_FLOAT;
        float cy = destructible.getY() + HEXAGON_HEIGHT * HALF_FLOAT;
        float r = HEXAGON_WIDTH * HALF_FLOAT - 2 + extra;

        float[] v = new float[12];
        for (int i = 0; i < 6; i++) {
            float angle = (float) (Math.PI / 3 * i);
            v[i * 2] = cx + (float) Math.cos(angle) * r;
            v[i * 2 + 1] = cy + (float) Math.sin(angle) * r;
        }
        for (int i = 0; i < 6; i++) {
            int next = (i + 1) % 6;
            sr.triangle(cx, cy, v[i * 2], v[i * 2 + 1], v[next * 2], v[next * 2 + 1]);
        }
    }
}
