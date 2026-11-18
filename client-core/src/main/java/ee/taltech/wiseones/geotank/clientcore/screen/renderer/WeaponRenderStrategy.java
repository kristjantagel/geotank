package ee.taltech.wiseones.geotank.clientcore.screen.renderer;

import ee.taltech.wiseones.geotank.clientcore.model.WeaponModel;

public interface WeaponRenderStrategy {
    boolean canRender(WeaponModel weapon);

    void render(WeaponModel weapon);
}