
## 0. Lobby Phase

These messages are exchanged during the lobby phase, before a game session is created.

### Lobby List & Creation

Clients request the list of available lobbies by sending a `LobbyListRequestMessage`. The server responds with a `LobbyListMessage` containing lobby information.

```java
public class LobbyListRequestMessage {
}

public class LobbyListMessage {
    private List<LobbyInfo> lobbies;
}

public class LobbyInfo {
    private int lobbyId;
    private String lobbyName;
    private int playerCount;
    private int maxPlayers;
    private boolean inGame;
}
```

To create a new lobby, a client sends a `CreateLobbyMessage`:

```java
public class CreateLobbyMessage {
    private String lobbyName;
    private String playerName;
    private int maxPlayers;
    private String code; // optional: null = public (shown in browser), non-null = private (code-only join)
}
```

### Joining a Lobby

Clients send a `JoinLobbyMessage` with the target lobby ID. After successfully joining (or creating) a lobby, the server broadcasts a `LobbyStateMessage` to all players in that lobby with the current list of players and their ready status.

```java
public class JoinLobbyMessage {
    private int lobbyId;
}

public class LobbyStateMessage {
    private List<LobbyPlayerInfo> players;
}

public class LobbyPlayerInfo {
    private int playerId;
    private String playerName;
    private boolean ready;
}
```

### Ready Status & Game Start

When a player is ready to play, they send a `PlayerReadyMessage`. The server broadcasts the updated `LobbyStateMessage` to all players. When all players in a lobby are ready, the server sends a `GameStartMessage` to all players, transitioning them from the lobby phase to the game phase.

```java
public class PlayerReadyMessage {
}

public class GameStartMessage {
}
```

### Additional Lobby Features

#### Private Lobby Access by Code

Clients can join private lobbies using a code sent by the host. Use `JoinByCodeMessage`:

```java
public class JoinByCodeMessage {
    private String code;
    private String playerName;
}
```

If the code is invalid or the lobby is full, the server responds with a `LobbyErrorMessage`:

```java
public class LobbyErrorMessage {
    private String reason;
}
```

#### Quick Join

Clients can request to join any available public lobby or have the server auto-create one:

```java
public class QuickJoinMessage {
    private String playerName;
}
```

#### Class Selection (Tank Classes)

During the game, players can upgrade their tank to a different class at certain milestones. The server broadcasts a `ClassSelectionPromptMessage` to notify a player they can upgrade:

```java
public class ClassSelectionPromptMessage {
    private int playerId;
    private TankClass[] availableClasses;
}

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

    private final int id;
    private final String displayName;
    
    /**
     * Get the next class in the adjacency wheel (wraps around).
     */
    public TankClass getNext() { ... }

    /**
     * Get the previous class in the adjacency wheel (wraps around).
     */
    public TankClass getPrevious() { ... }
}
```

The client responds with a `ClassSelectionMessage`. For first class selection (at level 10), only single classes are available. For subsequent upgrades, adjacent classes can be combined for multiclassing:

```java
public class ClassSelectionMessage {
    private TankClass primaryClass;
    private TankClass secondaryClass;  // null for single-class selections

    public ClassSelectionMessage(TankClass primaryClass, TankClass secondaryClass) { ... }
    public ClassSelectionMessage(TankClass primaryClass) { ... }
}
```

---

## 1. Connection Management

These messages handle players entering and exiting the game environment.

### Joining the Game

When a client wants to join, they send a `GameJoinMessage`. The server responds with a `WelcomeMessage` containing the initial configuration and the player's assigned IDs.

```java
public class GameJoinMessage {
}

public class WelcomeMessage {
    private int playerId;
    private int tankId;
    private List<PlayerTankConfig> playerTankConfigs;
}

public class PlayerTankConfig {
    private int playerId;
    private int tankId;
    private float width;
    private float height;
}
```

### Leaving the Game

When a client intends to disconnect, they send a `GameLeaveMessage`. The server then broadcasts this same message to all other connected clients to inform them of the departure.

```java
public class GameLeaveMessage {
    private int playerId;
}
```

---

## 2. Server Updates (State Synchronization)

The server is responsible for keeping all clients in sync regarding the world state and configuration changes.

### Game State Updates

During every game tick, the server broadcasts a `GameStateMessage` to all clients, detailing the current positions and rotations of all tanks, projectiles, and destructible objects.

```java
public class GameStateMessage {
    private List<PlayerTankState> playerTankStates;
    private List<ProjectileState> projectileStates;
    private List<DestructibleState> destructibleStates;
}

public class PlayerTankState {
    private int tankId;
    private float x;
    private float y;
    private float direction;
    private int lives;
    private int maxLives;
    private boolean alive;
    private int xp;
    private int level;
    private int score;
    private int skillPoints;
    private String name;

    @Override
    public String toString() {
        return String.format("PlayerTankState(%d, %f, %f, %f, %d, alive=%b, lvl=%d, xp=%d, score=%d, name=%s)",
                tankId, x, y, direction, lives, alive, level, xp, score, name);
    }
}

public class ProjectileState {
    private int id;
    private float x;
    private float y;
    private float direction;
}

public class DestructibleState {
    private int id;
    private float x;
    private float y;
    private int health;
    private int type;  // DestructibleType id
}

public enum DestructibleType {
    BOX(0, "Box"),
    SPHERE(1, "Sphere"),
    HEXAGON(2, "Hexagon");
    
    private final int id;
    private final String name;
}
```

### Configuration Changes

If any configurable properties of a player or tank change (e.g., a skin change or stat buff), the server broadcasts a `PlayerTankConfigMessage`.

```java
public class PlayerTankConfigMessage {
    PlayerTankConfig playerTankConfig;
}
```

### Stats Updates

When a player gains experience, levels up, changes score, or upgrades skills, the server broadcasts a `StatsUpdateMessage` to sync player progression and stats.

```java
public class StatsUpdateMessage {
    private int playerId;
    private int level;
    private int xp;
    private int score;
    private int skillPoints;           // Available points for upgrades
    private int[] skillLevels;         // 8 values, indexed by SkillType ordinal
    private TankClass primaryClass;    // The primary tank class
    private TankClass secondaryClass;  // Adjacent class for multiclassing, null for single-class
}
```

### Chat Messages

Players can send chat messages during the lobby phase. The server broadcasts all chat messages to all players in the lobby.

```java
public class ChatMessage {
    public static final int MAX_PLAYER_NAME_LENGTH = 15;
    
    private String playerName;
    private String message;
    private long timestamp;

    public ChatMessage(String playerName, String message) {
        // playerName and message are sanitized
    }
}
```

---

## 3. Client Input & Actions

The client sends these messages to the server during every game tick (or upon input change) to communicate player intent.

* **`TankFacingMessage`**: Sent every tick to update the turret or tank aim direction. Routed via UDP — dropped frames are acceptable for cursor aim.
* **`PlayerShootingMessage`**: Sent if the fire button is currently held down.
* **`TankMovementMessage`**: Sent whenever the desired movement direction changes.
* **`SkillUpgradeMessage`**: Sent when the player selects a stat to upgrade.
* **`RespawnRequestMessage`**: Sent when the player dies and requests respawn.

```java
public class TankFacingMessage {
    private float angleRad;
}

public class PlayerShootingMessage {
}

public class TankMovementMessage {
    private int directionMask;  // Bitmask for multiple simultaneous directions (supports diagonal movement)
}

/**
 * Direction bitmasks for movement. Multiple directions can be combined as a bitmask.
 */
public enum Direction {
    NONE(0),
    UP(1),
    DOWN(2),
    LEFT(4),
    RIGHT(8);
    
    private final int bitmask;
    
    public static boolean hasUp(int mask) { ... }
    public static boolean hasDown(int mask) { ... }
    public static boolean hasLeft(int mask) { ... }
    public static boolean hasRight(int mask) { ... }
}

public class SkillUpgradeMessage {
    private int statIndex; // 0-7 matching SkillType ordinal
}

public class RespawnRequestMessage {
}

/**
 * The 8 upgradeable stats.
 * Ordinal matches the statIndex in SkillUpgradeMessage.
 */
public enum SkillType {
    HEALTH_REGEN,
    MAX_HEALTH,
    BODY_DAMAGE,
    BULLET_SPEED,
    PIERCE,
    BULLET_DAMAGE,
    FIRE_RATE,
    MOVEMENT_SPEED
}
