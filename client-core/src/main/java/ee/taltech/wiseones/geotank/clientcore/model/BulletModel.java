package ee.taltech.wiseones.geotank.clientcore.model;

import ee.taltech.wiseones.geotank.shared.message.dto.ProjectileState;
import lombok.Getter;

@Getter
public class BulletModel {
    private final int id;
    private final float x;
    private final float y;

    public BulletModel(ProjectileState bulletDTO) {
        this.id = bulletDTO.getId();
        this.x = bulletDTO.getX();
        this.y = bulletDTO.getY();
    }
}
