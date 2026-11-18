package ee.taltech.wiseones.geotank.server.domain.model.terrain;


import ee.taltech.wiseones.geotank.server.domain.model.physics.Rect;

public record TerrainRegion(Rect bounds, TerrainProperties properties) {

    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    @Override
    public String toString() {
        return "TerrainRegion{" +
                "bounds=" + bounds +
                ", properties=" + properties +
                '}';
    }
}