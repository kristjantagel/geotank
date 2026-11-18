package ee.taltech.wiseones.geotank.clientcore.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.DEFAULT_EFFECTS_VOLUME;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.DEFAULT_MUSIC_VOLUME;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_HEIGHT;
import static ee.taltech.wiseones.geotank.shared.constant.Constants.VIEWPORT_WIDTH;

/**
 * Wraps LibGDX Preferences for persistent user settings. Values are flushed to disk
 * on every setter so we don't risk losing changes if the app closes uncleanly.
 */
public final class SettingsStore {
    private static final String PREFS_NAME = "geotank-settings";
    private static final String KEY_MUSIC = "musicVolume";
    private static final String KEY_EFFECTS = "effectsVolume";
    private static final String KEY_MASTER = "masterVolume";
    private static final String KEY_FULLSCREEN = "fullscreen";
    private static final String KEY_WINDOW_WIDTH = "windowWidth";
    private static final String KEY_WINDOW_HEIGHT = "windowHeight";
    private static final String KEY_VSYNC = "vsync";
    private static final String KEY_SHOW_FPS = "showFps";
    private static final String KEY_SHOW_MINIMAP = "showMinimap";
    private static final String KEY_SHOW_CHAT = "showChat";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_HUD_SCALE = "hudScale";

    private SettingsStore() {
    }

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    public static float getMusicVolume() {
        return prefs().getFloat(KEY_MUSIC, DEFAULT_MUSIC_VOLUME);
    }

    public static float getEffectsVolume() {
        return prefs().getFloat(KEY_EFFECTS, DEFAULT_EFFECTS_VOLUME);
    }

    public static float getMasterVolume() {
        return prefs().getFloat(KEY_MASTER, 1.0f);
    }

    public static boolean isFullscreen() {
        return prefs().getBoolean(KEY_FULLSCREEN, false);
    }

    public static int getWindowWidth() {
        return prefs().getInteger(KEY_WINDOW_WIDTH, VIEWPORT_WIDTH);
    }

    public static int getWindowHeight() {
        return prefs().getInteger(KEY_WINDOW_HEIGHT, VIEWPORT_HEIGHT);
    }

    public static boolean isVsync() {
        return prefs().getBoolean(KEY_VSYNC, true);
    }

    public static boolean isShowFps() {
        return prefs().getBoolean(KEY_SHOW_FPS, false);
    }

    public static boolean isShowMinimap() {
        return prefs().getBoolean(KEY_SHOW_MINIMAP, true);
    }

    public static boolean isShowChat() {
        return prefs().getBoolean(KEY_SHOW_CHAT, true);
    }

    public static String getUsername() {
        return prefs().getString(KEY_USERNAME, "");
    }

    public static float getHudScale() {
        return prefs().getFloat(KEY_HUD_SCALE, 1.0f);
    }

    public static void setMusicVolume(float v) {
        Preferences p = prefs();
        p.putFloat(KEY_MUSIC, v);
        p.flush();
    }

    public static void setEffectsVolume(float v) {
        Preferences p = prefs();
        p.putFloat(KEY_EFFECTS, v);
        p.flush();
    }

    public static void setMasterVolume(float v) {
        Preferences p = prefs();
        p.putFloat(KEY_MASTER, v);
        p.flush();
    }

    public static void setFullscreen(boolean isTrue) {
        Preferences p = prefs();
        p.putBoolean(KEY_FULLSCREEN, isTrue);
        p.flush();
    }

    public static void setWindow(int w, int h) {
        Preferences p = prefs();
        p.putInteger(KEY_WINDOW_WIDTH, w);
        p.putInteger(KEY_WINDOW_HEIGHT, h);
        p.flush();
    }

    public static void setVsync(boolean isTrue) {
        Preferences p = prefs();
        p.putBoolean(KEY_VSYNC, isTrue);
        p.flush();
    }

    public static void setShowFps(boolean isTrue) {
        Preferences p = prefs();
        p.putBoolean(KEY_SHOW_FPS, isTrue);
        p.flush();
    }

    public static void setShowMinimap(boolean isTrue) {
        Preferences p = prefs();
        p.putBoolean(KEY_SHOW_MINIMAP, isTrue);
        p.flush();
    }

    public static void setShowChat(boolean isTrue) {
        Preferences p = prefs();
        p.putBoolean(KEY_SHOW_CHAT, isTrue);
        p.flush();
    }

    public static void setUsername(String name) {
        Preferences p = prefs();
        p.putString(KEY_USERNAME, name == null ? "" : name);
        p.flush();
    }

    public static void setHudScale(float scale) {
        Preferences p = prefs();
        p.putFloat(KEY_HUD_SCALE, scale);
        p.flush();
    }
}
