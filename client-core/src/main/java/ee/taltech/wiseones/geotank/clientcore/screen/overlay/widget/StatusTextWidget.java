package ee.taltech.wiseones.geotank.clientcore.screen.overlay.widget;

import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudFrame;
import ee.taltech.wiseones.geotank.clientcore.screen.overlay.HudResources;

/**
 * Centered status text — used for "CONNECTION LOST" and similar transient banners.
 * Rendered in the player's eye-line so they can't miss it.
 */
public final class StatusTextWidget implements HudWidget {
    private final HudResources res;

    public StatusTextWidget(HudResources res) {
        this.res = res;
    }

    @Override
    public boolean visible(HudFrame frame) {
        String s = frame.viewModel().statusText();
        return s != null && !s.isEmpty();
    }

    @Override
    public void drawText(HudFrame frame) {
        String status = frame.viewModel().statusText();
        res.fontLarge.setColor(frame.viewModel().statusColor());
        res.glyphLayout.setText(res.fontLarge, status);
        float x = (frame.screenW() - res.glyphLayout.width) / 2f;
        float y = frame.screenH() / 2f;
        res.fontLarge.draw(res.spriteBatch, status, x, y);
    }
}
