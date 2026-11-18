package ee.taltech.wiseones.geotank.clientcore.screen.theme;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;

/**
 * Single source of truth for tank-class colors used anywhere on the client (HUD,
 * minimap, class-selection cards, future class indicators on tanks).
 * <p>
 * Each class has a distinct hue chosen to read clearly against the dark HUD
 * panels and to feel thematically appropriate (red = aggressive, blue =
 * defensive, etc.). DEFAULT and unknown classes fall back to neutral gray.
 */
public final class TankClassPalette {

    private static final Color DEFAULT_COLOR = new Color(0.61f, 0.64f, 0.69f, 1f);

    // Order matches TankClass enum so the palette reads top-to-bottom.
    private static final Color ARTILLERY = new Color(1.00f, 0.55f, 0.26f, 1f);
    private static final Color SPEEDSTER = new Color(0.36f, 0.85f, 1.00f, 1f);
    private static final Color MACHINE_GUN = new Color(1.00f, 0.39f, 0.39f, 1f);
    private static final Color TANK = new Color(0.36f, 0.55f, 1.00f, 1f);
    private static final Color STREAM = new Color(0.36f, 1.00f, 0.55f, 1f);
    private static final Color RAMMER = new Color(0.71f, 0.47f, 1.00f, 1f);
    private static final Color SNIPER = new Color(1.00f, 0.85f, 0.24f, 1f);

    private TankClassPalette() {
    }

    public static Color colorFor(TankClass tankClass) {
        if (tankClass == null) return DEFAULT_COLOR;
        return switch (tankClass) {
            case ARTILLERY -> ARTILLERY;
            case SPEEDSTER -> SPEEDSTER;
            case MACHINE_GUN -> MACHINE_GUN;
            case TANK -> TANK;
            case STREAM -> STREAM;
            case RAMMER -> RAMMER;
            case SNIPER -> SNIPER;
            case DEFAULT -> DEFAULT_COLOR;
        };
    }
}
