package ee.taltech.wiseones.geotank.clientcore.model;

import lombok.Getter;
import lombok.Setter;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TankModel {
    private int id;
    private String name;
    private float x;
    private float y;
    private float width;
    private float height;
    float rotation;
    private int lives;
    private int maxLives;
    private boolean alive = true;
    private int xp;
    private int level = 1;
    private int score;
    private int skillPoints;
    private int cannonCount = 1;
    private float spreadAngle = 0f;

    private final List<WeaponModel> weapons = new ArrayList<>();

    public TankModel(int id) {
        this.id = id;
        Logger.info("Created TankModel with ID: " + id);
    }

    @Override
    public String toString() {
        return String.format("TankModel(id: %d, x: %f, y: %f, rotation: %f)", id, x, y, rotation);
    }
}