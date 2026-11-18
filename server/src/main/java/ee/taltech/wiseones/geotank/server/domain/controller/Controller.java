package ee.taltech.wiseones.geotank.server.domain.controller;

import ee.taltech.wiseones.geotank.server.domain.model.tank.Tank;

public interface Controller {
    void update(Tank tank, float timeDeltaMs);
}
