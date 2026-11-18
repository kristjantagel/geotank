package ee.taltech.wiseones.geotank.server.domain.service;

import ee.taltech.wiseones.geotank.server.domain.model.terrain.TerrainProperties;
import ee.taltech.wiseones.geotank.server.domain.model.terrain.TerrainRegion;

import java.util.List;

public class TerrainService {

    private final List<TerrainRegion> regions;

    public TerrainService(List<TerrainRegion> regions) {
        this.regions = regions;
    }

    public TerrainProperties getPropertiesAt(float x, float y) {
        for (TerrainRegion region : regions) {
            if (region.contains(x, y)) {
                return region.properties();
            }
        }
        return TerrainProperties.defaultProperties();
    }
}