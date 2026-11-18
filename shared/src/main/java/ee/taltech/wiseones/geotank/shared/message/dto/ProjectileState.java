package ee.taltech.wiseones.geotank.shared.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectileState {
    private int id;

    private float x;
    private float y;
    private float direction;
}
