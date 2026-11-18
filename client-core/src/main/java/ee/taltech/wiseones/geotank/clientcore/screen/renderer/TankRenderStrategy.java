package ee.taltech.wiseones.geotank.clientcore.screen.renderer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ee.taltech.wiseones.geotank.clientcore.model.TankModel;

public interface TankRenderStrategy {
    boolean canRender(TankModel tank); // tells if this strategy fits the given tank

    void render(TankModel tank, SpriteBatch spriteBatch, ShapeRenderer shapeRenderer);
}