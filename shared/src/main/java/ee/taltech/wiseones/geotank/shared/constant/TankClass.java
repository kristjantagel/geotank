package ee.taltech.wiseones.geotank.shared.constant;

import lombok.Getter;

/**
 * Tank class selection system - 7 classes in an adjacency wheel.
 * Classes can be combined with adjacent ones for multiclassing.
 * Adjacency Wheel:
 * 1. Artillery → 2. Speedster → 3. Machine Gun → 4. Tank → 5. Stream → 6. Rammer → 7. Sniper → (wraps to 1)
 */
public enum TankClass {
    DEFAULT(0, "Default"),
    ARTILLERY(1, "Artillery"),           // +Cannons, +Spread
    SPEEDSTER(2, "Speedster"),           // +Move Speed, +Bullet Speed
    MACHINE_GUN(3, "Machine Gun"),       // +Fire Rate, -Bullet Lifetime
    TANK(4, "Tank"),                     // +Max HP, +HP Regen, -Move Speed
    STREAM(5, "Stream"),                 // +Bullets, -Bullet Speed, -Spread
    RAMMER(6, "Rammer"),                 // +Body Damage, +Max HP
    SNIPER(7, "Sniper");                 // +Bullet Damage, +Pierce, -Fire Rate, +Bullet Lifetime

    @Getter
    private final int id;
    @Getter
    private final String displayName;

    TankClass(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Get the next class in the adjacency wheel (wraps around).
     */
    public TankClass getNext() {
        if (this == DEFAULT) return DEFAULT;
        return TankClass.values()[(this.id % 7) + 1];
    }

    /**
     * Get the previous class in the adjacency wheel (wraps around).
     */
    public TankClass getPrevious() {
        if (this == DEFAULT) return DEFAULT;
        int prevId = this.id - 1;
        if (prevId < 1) prevId = 7;
        return TankClass.values()[prevId];
    }

}
