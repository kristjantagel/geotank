package ee.taltech.wiseones.geotank.shared.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SkillUpgradeMessage {
    private int statIndex; // 0-7 matching SkillType ordinal
}
