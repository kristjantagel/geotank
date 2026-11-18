package ee.taltech.wiseones.geotank.clientcore.screen.overlay;

import ee.taltech.wiseones.geotank.clientcore.viewmodel.HudViewModel;

/**
 * Per-frame snapshot passed to widgets during render.
 *
 * Bundles the virtual canvas dimensions and the immutable view model so
 * widgets don't have to re-read settings or query Gdx each frame.
 */
public record HudFrame(HudViewModel viewModel, float screenW, float screenH, float scale) {
}
