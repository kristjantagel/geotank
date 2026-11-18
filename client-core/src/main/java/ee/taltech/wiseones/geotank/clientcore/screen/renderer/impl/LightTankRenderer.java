package ee.taltech.wiseones.geotank.clientcore.screen.renderer.impl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import ee.taltech.wiseones.geotank.clientcore.model.TankModel;
import ee.taltech.wiseones.geotank.clientcore.screen.renderer.TankRenderStrategy;

import static ee.taltech.wiseones.geotank.clientcore.util.Font.getArialFont;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.HALF_FLOAT;

/**
 * Diep.io-style tank renderer: gray rectangular barrel behind a colored circular body,
 * with dark outlines on both. Health bar and level label drawn above the tank.
 */
public class LightTankRenderer implements TankRenderStrategy {

    // Diep.io tank colors
    private static final Color BODY_FILL = new Color(0.0f, 0.72f, 0.92f, 1f);    // blue (#00B8EB)
    private static final Color BODY_OUTLINE = new Color(0.0f, 0.50f, 0.65f, 1f);  // darker blue border
    private static final Color BARREL_FILL = new Color(0.60f, 0.60f, 0.60f, 1f);  // gray
    private static final Color BARREL_OUTLINE = new Color(0.40f, 0.40f, 0.40f, 1f);
    private static final Color HP_BAR_BG = new Color(0.15f, 0.15f, 0.15f, 0.6f);
    private static final Color HP_BAR_FILL = new Color(0.33f, 0.85f, 0.33f, 1f);  // green
    private static final Color HP_BAR_LOW = new Color(0.95f, 0.25f, 0.25f, 1f);   // red when low
    private static final float OUTLINE_WIDTH = 2.5f;
    private static final float HP_BAR_WIDTH = 36f;
    private static final float HP_BAR_HEIGHT = 5f;
    private static final float HP_BAR_OFFSET_Y = 4f;  // above the tank body

    private final BitmapFont labelFont;
    private final GlyphLayout glyphLayout;

    public LightTankRenderer() {
        this.labelFont = getArialFont(9);
        this.glyphLayout = new GlyphLayout();
    }

    @Override
    public boolean canRender(TankModel tank) {
        return true;
    }

    @Override
    public void render(TankModel tank, SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
        float cx = tank.getX();
        float cy = tank.getY();
        float radius = Math.max(tank.getWidth(), tank.getHeight()) * HALF_FLOAT;
        float angleDeg = (float) Math.toDegrees(tank.getRotation());

        // Barrel dimensions (rectangular, extends from center forward)
        float barrelLength = radius * 1.5f;
        float barrelWidth = radius * 0.7f;

        // Mirror SimpleTank.fireFromCannon: each cannon points at facing + offset, where
        // offset spans the effective spread angle evenly across cannonCount barrels.
        int cannonCount = Math.max(1, tank.getCannonCount());
        float effectiveSpread = Math.max(1f, tank.getSpreadAngle());
        if (effectiveSpread * cannonCount >= 360f) effectiveSpread = 360f / cannonCount;

        // --- Save transform, apply rotation around tank center ---
        Matrix4 oldTransform = new Matrix4(shapeRenderer.getTransformMatrix());

        // Draw barrels FIRST (behind the body)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < cannonCount; i++) {
            float cannonOffsetDeg = (cannonCount > 1)
                    ? (i - (cannonCount - 1) / 2f) * effectiveSpread
                    : 0f;

            shapeRenderer.identity();
            shapeRenderer.translate(cx, cy, 0);
            shapeRenderer.rotate(0, 0, 1, angleDeg + cannonOffsetDeg);

            // Barrel outline (slightly larger rect behind the fill)
            shapeRenderer.setColor(BARREL_OUTLINE);
            shapeRenderer.rect(
                    -OUTLINE_WIDTH,
                    -barrelWidth * HALF_FLOAT - OUTLINE_WIDTH,
                    barrelLength + OUTLINE_WIDTH * 2,
                    barrelWidth + OUTLINE_WIDTH * 2
            );
            // Barrel fill
            shapeRenderer.setColor(BARREL_FILL);
            shapeRenderer.rect(0, -barrelWidth * HALF_FLOAT, barrelLength, barrelWidth);
        }

        shapeRenderer.end();

        // Restore transform for body (drawn at world position, no rotation needed for circle)
        shapeRenderer.setTransformMatrix(oldTransform);

        // Draw body circle with outline
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Body outline (larger circle behind)
        shapeRenderer.setColor(BODY_OUTLINE);
        shapeRenderer.circle(cx, cy, radius + OUTLINE_WIDTH, 32);
        // Body fill
        shapeRenderer.setColor(BODY_FILL);
        shapeRenderer.circle(cx, cy, radius, 32);
        shapeRenderer.end();

        // --- Health bar above the tank ---
        float hpRatio = (tank.getMaxLives() > 0) ? (float) tank.getLives() / tank.getMaxLives() : 1f;
        // Only show health bar if damaged
        if (hpRatio < 1f) {
            float hpX = cx - HP_BAR_WIDTH * HALF_FLOAT;
            float hpY = cy + radius + HP_BAR_OFFSET_Y;

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            // Background
            shapeRenderer.setColor(HP_BAR_BG);
            shapeRenderer.rect(hpX, hpY, HP_BAR_WIDTH, HP_BAR_HEIGHT);
            // Fill (green or red when low)
            Color hpColor = hpRatio > 0.3f ? HP_BAR_FILL : HP_BAR_LOW;
            shapeRenderer.setColor(hpColor);
            shapeRenderer.rect(hpX, hpY, HP_BAR_WIDTH * Math.max(0, hpRatio), HP_BAR_HEIGHT);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // --- Level label above the tank (or above HP bar) ---
        float labelY = cy + radius + HP_BAR_OFFSET_Y + HP_BAR_HEIGHT + 10f;
        String levelText = "Lv " + tank.getLevel();
        glyphLayout.setText(labelFont, levelText);
        spriteBatch.begin();
        labelFont.setColor(Color.WHITE);
        labelFont.draw(spriteBatch, levelText, cx - glyphLayout.width * HALF_FLOAT, labelY);
        spriteBatch.end();
    }
}
