package ee.taltech.wiseones.geotank.desktoplauncher;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import ee.taltech.wiseones.geotank.clientcore.GeoTankMain;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Launches the game client application
 */
public class ClientLauncher {
    public static final int DEPTH = 16;

    public static void main(String[] args) {
        if (ee.taltech.wiseones.geotank.desktoplauncher.StartupHelper.isStartJvmIfRequired())
            return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static void createApplication() {
        new Lwjgl3Application(new GeoTankMain(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("example-game");
        // Vsync limits the frames per second to what your hardware can display, and helps eliminate
        // screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
        configuration.useVsync(true);
        // Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
        // refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        // If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        // useful for testing performance, but can also be very stressful to some hardware.
        // You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.
        configuration.setWindowedMode(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        // 4x MSAA — smooths edges on ShapeRenderer-drawn tanks/bullets/destructibles
        // and any alpha-blended UI. Defaults are RGBA8/depth16/stencil0; only samples changes.
        configuration.setBackBufferConfig(8, 8, 8, 8, DEPTH, 0, 4);
        // You can change these files; they are in lwjgl3/src/main/resources/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
