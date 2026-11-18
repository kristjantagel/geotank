package ee.taltech.wiseones.geotank.server.domain.model.physics;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.HALF_FLOAT;

public record Rect(float x, float y, float width, float height) {

    /**
     * Factory method to create a bounding box from center coordinates and dimensions.
     * The resulting rectangle is positioned at the bottom-left corner.
     *
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param width rectangle width
     * @param height rectangle height
     * @return a bounding box with bottom-left corner positioned appropriately
     */
    public static Rect fromCenter(float centerX, float centerY, float width, float height) {
        return new Rect(
                centerX - width * HALF_FLOAT,
                centerY - height * HALF_FLOAT,
                width,
                height
        );
    }

    /**
     * Is point (px, py) inside the rectangle?
     */
    public boolean contains(float px, float py) {
        return px >= x && px <= x + width
                && py >= y && py <= y + height;
    }

    /**
     * Is the other rectangle completely inside?
     */
    public boolean contains(Rect other) {
        if (other == null) return false;

        return this.contains(other.x, other.y) &&
                this.contains(other.x + other.width, other.y + other.height);
    }

    /**
     * Do two axis-aligned rectangles intersect?
     */
    public boolean isIntersecting(Rect other) {
        if (other == null) {
            return false;
        }

        return this.x < other.x + other.width &&
                this.x + this.width > other.x &&
                this.y < other.y + other.height &&
                this.y + this.height > other.y;
    }

    @Override
    public String toString() {
        return "Rect{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
