package ee.taltech.wiseones.geotank.clientcore.viewmodel;

import com.badlogic.gdx.graphics.Color;
import ee.taltech.wiseones.geotank.shared.constant.TankClass;
import ee.taltech.wiseones.geotank.shared.message.dto.DestructibleType;

import java.util.List;

public record HudViewModel(
        // Status
        String statusText, Color statusColor,
        // Local player health
        int localLives, int localMaxLives,
        // Local player progression
        int localXp, int localLevel, int localScore, int localSkillPoints,
        int xpForCurrentLevel, int xpForNextLevel,
        // Skill stat levels (8 values, indexed by SkillType ordinal)
        int[] skillLevels,
        // Local player's currently selected class(es) — secondary may be null
        TankClass localPrimaryClass,
        TankClass localSecondaryClass,
        // Leaderboard
        List<LeaderboardEntry> leaderboard,
        // Local player alive state
        boolean localAlive,
        // Class upgrade modal
        boolean classSelectionActive,
        TankClass[] availableClasses,
        TankClass[] availableSecondaryClasses,
        // Minimap (world-space coords; Hud scales them into the minimap rect)
        List<MinimapTank> minimapTanks,
        List<MinimapDestructible> minimapDestructibles,
        // Pause overlay
        boolean paused,
        // Settings-driven toggles and metrics
        boolean showMinimap,
        boolean showFps,
        int fps
) {
    public record LeaderboardEntry(String name, int score, int level, boolean isLocal) {
    }

    public record MinimapTank(float x, float y, boolean isLocal, boolean alive) {
    }

    public record MinimapDestructible(float x, float y, DestructibleType type) {
    }
}
