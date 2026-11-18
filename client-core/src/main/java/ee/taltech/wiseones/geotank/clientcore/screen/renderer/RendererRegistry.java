package ee.taltech.wiseones.geotank.clientcore.screen.renderer;

import ee.taltech.wiseones.geotank.clientcore.model.TankModel;
import ee.taltech.wiseones.geotank.clientcore.model.WeaponModel;
import org.reflections.Reflections;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RendererRegistry {

    private final List<TankRenderStrategy> tankStrategies;
    private final List<WeaponRenderStrategy> weaponStrategies;

    public RendererRegistry() {
        tankStrategies = findStrategies(TankRenderStrategy.class);
        weaponStrategies = findStrategies(WeaponRenderStrategy.class);
    }

    private static <T> List<T> findStrategies(Class<T> strategyInterface) {
        Reflections reflections = new Reflections(strategyInterface.getPackage().getName());

        Set<Class<? extends T>> strategyClasses = reflections.getSubTypesOf(strategyInterface);

        return strategyClasses.stream()
                .map(cl -> {
                    try {
                        return cl.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Cannot instantiate strategy: " + cl.getName(), e);
                    }
                })
                .collect(Collectors.toList());
    }

    public Optional<TankRenderStrategy> getTankStrategy(TankModel tank) {
        return tankStrategies.stream()
                .filter(strategy -> strategy.canRender(tank))
                .findFirst();
    }

    public Optional<WeaponRenderStrategy> getWeaponStrategy(WeaponModel weapon) {
        return weaponStrategies.stream()
                .filter(strategy -> strategy.canRender(weapon))
                .findFirst();
    }
}