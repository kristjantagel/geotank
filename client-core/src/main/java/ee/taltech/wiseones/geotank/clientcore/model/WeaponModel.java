package ee.taltech.wiseones.geotank.clientcore.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class WeaponModel {

    private final int typeId;
    @Setter
    private float angle;

    public WeaponModel(int typeId) {
        this.typeId = typeId;
    }
}