package ee.taltech.wiseones.geotank.server.domain.model.physics;

/**
 * Simple 2D vector with X/Y components.
 * Immutable, usable for speed, acceleration etc.
 */
public record Vector2(float x, float y) {

    /**
     * Vector length
     */
    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Scales the vector
     */
    public Vector2 scale(float factor) {
        return new Vector2(x * factor, y * factor);
    }

    /**
     * Adds another vector
     */
    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }

    /**
     * Subtracts another vector
     */
    public Vector2 subtract(Vector2 other) {
        return new Vector2(this.x - other.x, this.y - other.y);
    }

    /**
     * Limits the vector size to maxMagnitude value, preserving direction
     */
    public Vector2 limit(float maxMagnitude) {
        float mag = this.magnitude();
        if (mag > maxMagnitude) {
            return this.scale(maxMagnitude / mag);
        }
        return this;
    }
}
