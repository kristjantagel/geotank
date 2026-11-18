package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;

/** Bottom-right FPS readout. Hidden unless the player enables it in settings. */
public final class FpsCounterWidget implements HudWidget {
    private final HudResources res;

    public FpsCounterWidget(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean visible(HudFrame frame) {
        return frame.viewModel().showFps();
    }

    @Override
    public void drawText(HudFrame frame) {
        String text = "FPS: " + frame.viewModel().fps();
        res.glyphLayout.setText(res.fontSmall, text);
        float x = frame.screenW() - res.glyphLayout.width - 10f;
        float y = 18f;
        res.fontSmall.setColor(Color.WHITE);
        res.fontSmall.draw(res.spriteBatch, text, x, y);
    }
}
