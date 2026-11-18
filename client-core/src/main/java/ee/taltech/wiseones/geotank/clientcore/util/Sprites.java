package ee.taltech.wiseones.geotank.clientcore.util;

import com.badlogic.gdx.graphics.Texture;

public class Sprites {
    private Sprites() {
    }

    // Not deleted due to potential usefulness later
    public static final Texture playerTexture = load("sprites/koalio-single.png");
    public static final Texture bulletTexture = load("sprites/bullet.png");
    public static final Texture taltechMapTexture = load("sprites/taltech-map.png");

    private static Texture load(String path) {
        Texture t = new Texture(path);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }
}
