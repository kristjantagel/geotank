package ee.taltech.wiseones.geotank.shared.constant;

public class Constants {

    private Constants() {
    }

    public static final float HALF_FLOAT = 0.5f;

    // --- networking constants ---
    public static final int PORT_TCP = 8080;
    public static final int PORT_UDP = 8081;
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int WRITE_BUFFER_SIZE = 65536;
    // this should be changed depending on where the server is hosted
    public static final String SERVER_IP = "localhost";

    // --- ui constants ---
    public static final float UI_BG_COLOR_R = 192 / 255f;
    public static final float UI_BG_COLOR_G = 192 / 255f;
    public static final float UI_BG_COLOR_B = 192 / 255f;
    public static final float UI_BG_COLOR_A = 1f;
    public static final int CIRCLE_SEGMENTS = 24;


    // --- player constants ---
    public static final int PLAYER_LIVES_COUNT = 50;
    public static final float FRICTION_COEFFICIENT = 0.15f;
    public static final float MIN_MOVEMENT = 0.01f; // Reduce sliding

    // --- audio constants ---
    public static final float DEFAULT_MUSIC_VOLUME = 0.05f;
    public static final float DEFAULT_EFFECTS_VOLUME = 0.05f;


    // --- bullet constants ---
    public static final float BULLET_WIDTH_IN_PIXELS = 8;
    public static final float BULLET_HEIGHT_IN_PIXELS = 8;

    // --- tank contact (body) collision constants ---
    public static final long TANK_CONTACT_DAMAGE_COOLDOWN_MS = 500;
    // Coefficient of restitution for tank-vs-tank bounce (0 = inelastic, 1 = elastic)
    public static final float TANK_BOUNCE_COEFFICIENT = 0.4f;
    // Baseline separation impulse applied on any contact, so even stationary touches push apart
    public static final float TANK_BOUNCE_IMPULSE = 30f;

    // --- class upgrade progression constants ---
    public static final int CLASS_UPGRADE_INTERVAL_LEVELS = 10;   // Every 10 levels (10, 20, 30, 40)

    // --- power level (PL) constants ---
    public static final int BASE_POWER = 100;                    // Base power for level 1
    public static final int POWER_PER_LEVEL = 50;                // PL increase per class upgrade level
    public static final float PL_TOLERANCE = 1.0f;               // Bisection search tolerance
    public static final int BISECTION_ITERATIONS = 20;           // Max iterations for bisection
    public static final float PL_OFFENSE_ALPHA = 1.1f;           // Global scalar for offensive PL (>1)
    public static final float PL_RECOIL_BETA = 0.5f;             // Recoil weight in PL formula
    public static final float MAX_RECOIL = 10f;                  // Cap for recoil vector length

    // --- weight system constants ---
    public static final float STARTING_WEIGHT = 2.0f;            // All stats start at this weight
    public static final float WEIGHT_DECAY_FACTOR = 0.5f;        // Weight decays by 0.5 toward 1.0
    public static final float TARGET_WEIGHT = 1.0f;              // Weights naturally decay to this
    public static final float MIN_LEVERAGE_THRESHOLD = 1f;       // Minimum leverage before fallback
    public static final float DEFAULT_GROWTH = 0.1f;             // Default growth weight when leverage is too low
    public static final float MIN_STAT_MULTIPLIER_BISECTION = -100.0f;  // Minimum X multiplier for bisection
    public static final float MAX_STAT_MULTIPLIER_BISECTION = 100.0f;   // Maximum X multiplier for bisection
    public static final float MIN_STAT_VALUE = 1.0f;             // Minimum value for any stat

    // --- base stat values (used in weight formulas) ---
    // Bullet dmg = PL / 10 + skill * mod
    // Regen = PL / 5 + skill * mod
    public static final int BASE_HP_REGEN = 5;                   // HP regeneration per second
    public static final int BASE_HEALTH = 50;                    // BASE_HEALTH
    public static final float BASE_BODY_DAMAGE = 10f;            // Body collision damage
    public static final float BASE_BULLET_SPEED = 200f;          // Bullet speed
    public static final float BASE_PIERCE = 1f;                  // Pierce/Penetration
    public static final float BASE_BULLET_DAMAGE = 10f;          // Bullet damage
    public static final float BASE_FIRE_RATE = 4f;               // Fire rate (bullets per second)
    public static final float BASE_MOVE_SPEED = 750f;            // Movement speed (replaces max speed & acceleration)
    public static final float BASE_BULLET_LIFETIME = 2000f;      // Bullet lifetime in milliseconds
    // discrete/special
    public static final int BASE_CANNONS = 1;                    // Number of cannons
    public static final float BASE_BULLETS = 1.0f;               // Bullets per cannon (in increments of 0.5)
    public static final float BASE_SPREAD = 0f;                  // Base spread angle in degrees

    // --- class modifier weights (what makes each class unique) ---
    // Weight modifiers are applied to the baseline weights during class upgrades.
    // Format: Each stat can be increased or decreased depending on the class identity.
    // Artillery:      +Cannons(discrete), +Spread(special)
    // Speedster:      +Move Speed, +Bullet Speed
    // Machine Gun:    +Fire Rate, -Bullet Lifetime
    // Tank:           +Max HP, +HP Regen, -Move Speed
    // Stream:         +Bullets(discrete), -Bullet Speed, -Spread(special)
    // Rammer:         +Body Damage, +Max HP
    // Sniper:         +Bullet Damage, +Pierce, -Fire Rate, +Bullet Lifetime

    // Discrete/Special modifiers (whole number or 0.5 increments)
    public static final int CLASS_ARTILLERY_CANNON_MOD = 1;      // +1 cannon
    public static final float CLASS_ARTILLERY_SPREAD_MOD = 30f;   // Increase cannon spread

    public static final float CLASS_STREAM_BULLETS_MOD = 0.5f;        // +0.5 bullets per cannon
    public static final float CLASS_STREAM_SPREAD_MOD = -15f;      // Decrease cannon spread

    // Continuous weight modifiers (+1.0 or -1.0 applied to baseline weights)
    public static final float CLASS_SPEEDSTER_MOVE_SPEED_MOD = 1.0f;
    public static final float CLASS_SPEEDSTER_BULLET_SPEED_MOD = 1.0f;

    public static final float CLASS_MACHINEGUN_FIRE_RATE_MOD = 1.0f;
    public static final float CLASS_MACHINEGUN_BULLET_LIFETIME_MOD = -1.25f;

    public static final float CLASS_TANK_HEALTH_MOD = 1.0f;
    public static final float CLASS_TANK_REGEN_MOD = 1.0f;
    public static final float CLASS_TANK_MOVE_SPEED_MOD = -1.25f;

    public static final float CLASS_STREAM_BULLET_SPEED_MOD = -1.25f;

    public static final float CLASS_RAMMER_BODY_DAMAGE_MOD = 1.0f;
    public static final float CLASS_RAMMER_HEALTH_MOD = 1.0f;

    public static final float CLASS_SNIPER_BULLET_DAMAGE_MOD = 1.0f;
    public static final float CLASS_SNIPER_PIERCE_MOD = 1.0f;
    public static final float CLASS_SNIPER_FIRE_RATE_MOD = -0.75f;
    public static final float CLASS_SNIPER_BULLET_LIFE_MOD = 1.0f;

    // --- PL utility coefficients (lambda values for each stat) ---
    public static final float PL_LAMBDA_HP = 0.01f;               // PL cost per HP
    public static final float PL_LAMBDA_REGEN = 1.0f;            // PL cost per HP regen
    public static final float PL_LAMBDA_BODY_DAMAGE = 1.0f;      // PL cost per body damage
    public static final float PL_LAMBDA_MOVE_SPEED = 0.01f;       // PL cost per move speed unit
    public static final float PL_LAMBDA_BULLET_SPEED = 0.01f;     // PL cost per bullet speed unit
    public static final float PL_LAMBDA_LIFETIME = 0.01f;         // PL cost per lifetime unit

    // --- skill system constants (diep.io style stat upgrades) ---
    public static final float SKILL_HEALTH_REGEN_BONUS = 1f; // Regen rate per skill point
    public static final float SKILL_MAX_HEALTH_MULTIPLIER = 0.14f;     // Bonus per skill point (14%)
    public static final int SKILL_BODY_DAMAGE_BONUS = 1;           // Bonus per skill point
    public static final float SKILL_BULLET_SPEED_MULTIPLIER = 0.15f;   // Bonus per skill point (15%)
    public static final int SKILL_BULLET_PIERCE_BONUS = 1;         // Bonus per skill point
    public static final int SKILL_BULLET_DAMAGE_BONUS = 1;         // Bonus per skill point
    public static final float SKILL_FIRE_RATE_REDUCTION = 0.10f;     // Reduction per skill point (10%)
    public static final float SKILL_MOVEMENT_SPEED_MULTIPLIER = 0.15f; // Bonus per skill point (15%)

    // --- lobby constants ---
    // 1 = solo play allowed (AISpawner fills 2 bots in for a 1-human lobby).
    public static final int MAX_PLAYERS_PER_LOBBY = 4;
    public static final int MIN_PLAYERS_CUSTOM_LOBBY = 2;
    public static final int MAX_PLAYERS_CUSTOM_LOBBY = 16;
    public static final int MAX_PLAYERS_AUTO_LOBBY = 32;
    public static final long AI_RECONCILE_INTERVAL_MS = 1000;

    // AI population cap scales with human count so small parties don't drown in bots:
    // humanCount ≤ threshold → AI cap = humanCount * LOW multiplier
    // humanCount >  threshold → AI cap = humanCount * HIGH multiplier (floor).
    // The lobby's targetTotalPlayers still applies as an upper bound.
    public static final int AI_CAP_HUMAN_THRESHOLD = 4;
    public static final float AI_CAP_MULTIPLIER_LOW = 2.0f;
    public static final float AI_CAP_MULTIPLIER_HIGH = 1.5f;

    // HUD leaderboard: top N scorers are shown. If the local player isn't already in
    // the top N, they're appended as an extra entry so the user always sees their row.
    public static final int LEADERBOARD_MAX_ENTRIES = 5;

    // --- game constants ---
    public static final int GAME_TICK_RATE = 60;
    // World size — the full playable area on the server
    public static final int ARENA_LOWER_BOUND_X = 0;
    public static final int ARENA_UPPER_BOUND_X = 3200;
    public static final int ARENA_LOWER_BOUND_Y = 0;
    public static final int ARENA_UPPER_BOUND_Y = 2400;
    public static final int ARENA_BORDER_PADDING = 20;
    // Default client window size and minimum world for the gameplay camera.
    // 1280×720 matches the supported widescreen resolutions in SettingsScreen
    // and gives the menu layouts enough room to breathe on modern monitors.
    public static final int VIEWPORT_WIDTH = 1280;
    public static final int VIEWPORT_HEIGHT = 720;

    // --- destructible object constants ---
    public static final float DESTRUCTIBLE_WIDTH = 32;
    public static final float DESTRUCTIBLE_HEIGHT = 32;
    public static final int DESTRUCTIBLES_PER_ARENA = 60;
    // --- destructible type 1: box ---
    public static final float BOX_WIDTH = 32;
    public static final float BOX_HEIGHT = 32;
    public static final int BOX_HEALTH = 1;
    public static final int BOX_XP_REWARD = 10;
    public static final int BOX_SCORE_REWARD = 50;
    public static final float BOX_SPAWN_CHANCE = 0.4f; // 40% of spawned destructibles

    // --- destructible type 2: sphere ---
    public static final float SPHERE_WIDTH = 32;
    public static final float SPHERE_HEIGHT = 32;
    public static final float SPHERE_RADIUS = 16;
    public static final int SPHERE_HEALTH = 2;
    public static final int SPHERE_XP_REWARD = 25;
    public static final int SPHERE_SCORE_REWARD = 100;
    public static final float SPHERE_SPAWN_CHANCE = 0.3f; // 30% of spawned destructibles

    // --- destructible type 3: hexagon ---
    public static final float HEXAGON_WIDTH = 32;
    public static final float HEXAGON_HEIGHT = 32;
    public static final int HEXAGON_HEALTH = 3;
    public static final int HEXAGON_XP_REWARD = 50;
    public static final int HEXAGON_SCORE_REWARD = 200;

    // --- destructible respawn constants ---
    public static final long DESTRUCTIBLE_RESPAWN_DELAY_MS = 5000;
    public static final int MAX_DESTRUCTIBLES_IN_ARENA = 80;

    // --- leveling constants (diep.io style) ---
    public static final int MAX_LEVEL = 45;
    public static final int MAX_SKILL_STAT_LEVEL = 7;
    public static final int MAX_KILL_XP = 23537;
    public static final int BASE_KILL_XP = 50;

    // --- tank movement constants (acceleration derived from movement speed) ---
    // Acceleration is derived from movement speed: acceleration = moveSpeed * ACCELERATION_MULTIPLIER
    public static final float ACCELERATION_MULTIPLIER = 0.67f;  // acceleration = moveSpeed * 0.67

    // --- tank physics properties constants ---
    public static final float TANK_WIDTH = 40f;                 // Tank body width in pixels
    public static final float TANK_HEIGHT = 20f;                // Tank body height in pixels
    public static final float TANK_PHYSICS_MAX_SPEED = 200f;    // Max speed for physics engine

    // --- AI constants ---
    public static final float AI_PLAYER_DETECTION_RANGE = 500f;
    public static final float AI_BOUNDARY_BUFFER = 150f; // Distance to maintain from arena walls

    // --- Chat and username constants ---
    public static final int MAX_MESSAGE_LENGTH = 200;
    public static final int MAX_USERNAME_LENGTH = 15;
}
