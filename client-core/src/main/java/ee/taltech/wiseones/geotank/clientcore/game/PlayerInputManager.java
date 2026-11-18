package ee.taltech.wiseones.geotank.clientcore.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import ee.taltech.wiseones.geotank.clientcore.audio.AudioManager;
import ee.taltech.wiseones.geotank.clientcore.model.TankModel;
import ee.taltech.wiseones.geotank.clientcore.network.NetworkPlayerCommandHandler;
import ee.taltech.wiseones.geotank.shared.message.dto.Direction;
import ee.taltech.wiseones.geotank.shared.util.Logger;

/**
 * Listens to user input and forwards commands to their handler.
 * <p>
 * Class-select keys are mapped by index into the server-supplied available-
 * classes array, never by hardcoded enum, so the level-20+ "prev | current
 * | next" 3-class menu picks the right class.
 */
public class PlayerInputManager {
    private static final int[] CLASS_SELECT_KEYS = {
            Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4,
            Keys.NUM_5, Keys.NUM_6, Keys.NUM_7
    };

    private final NetworkPlayerCommandHandler commandHandler;
    private final AudioManager audioManager;
    private int previousDirectionMask = 0;
    private long lastShotTime = 0;
    private static final long SHOOT_COOLDOWN_MS = 200; // Match server-side cooldown

    public PlayerInputManager(NetworkPlayerCommandHandler commandHandler, AudioManager audioManager) {
        this.commandHandler = commandHandler;
        this.audioManager = audioManager;
    }

    /**
     * Sends a NONE movement to the server and resets the cached direction so the next
     * key press re-sends the actual direction. Used when entering pause so the tank
     * stops on the server side.
     */
    public void stopMovement() {
        previousDirectionMask = Direction.NONE.getBitmask();
        commandHandler.onMove(previousDirectionMask);
    }

    public void handleMovementInput() {
        int currentDirectionMask = getMovingDirectionMask();

        // send only if direction changed
        if (currentDirectionMask != previousDirectionMask) {
            previousDirectionMask = currentDirectionMask;
            Logger.debug("Movement direction changed to: " + Integer.toBinaryString(currentDirectionMask));
            commandHandler.onMove(currentDirectionMask);
        }
    }

    /**
     * Get direction as bitmask to support diagonal movement
     * Allows multiple directions to be pressed simultaneously
     */
    private int getMovingDirectionMask() {
        int mask = Direction.NONE.getBitmask();

        if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) {
            mask |= Direction.UP.getBitmask();
        }
        if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) {
            mask |= Direction.DOWN.getBitmask();
        }
        if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) {
            mask |= Direction.LEFT.getBitmask();
        }
        if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) {
            mask |= Direction.RIGHT.getBitmask();
        }

        return mask;
    }

    public void handleShootingInput() {
        if (!Gdx.input.isKeyPressed(Keys.SPACE)) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= SHOOT_COOLDOWN_MS) {
            lastShotTime = currentTime;
            audioManager.playShootSound();
            commandHandler.onShoot();
        }
    }

    // --- Facing angle towards mouse (unprojected to world space via camera) ---
    public void handleFacingInput(TankModel localTank, OrthographicCamera camera) {
        if (localTank == null) return;

        // Unproject mouse from screen coordinates to world coordinates
        Vector3 mouseWorld = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        // Calculate angle from tank center to mouse in world space
        float angleRad = (float) Math.atan2(mouseWorld.y - localTank.getY(), mouseWorld.x - localTank.getX());

        commandHandler.onRotate(angleRad);
    }

    /**
     * Returns the 0-based index of the number-row key pressed for class selection,
     * or -1 if none. Caller is responsible for resolving the (primary, secondary)
     * tuple from the chosen card and sending the ClassSelectionMessage.
     */
    public int pickedClassIndex(int cardCount) {
        if (cardCount <= 0) return -1;
        int max = Math.min(cardCount, CLASS_SELECT_KEYS.length);
        for (int i = 0; i < max; i++) {
            if (Gdx.input.isKeyJustPressed(CLASS_SELECT_KEYS[i])) return i;
        }
        return -1;
    }
}