package dev.breezes.settlements.bootstrap.registry.sensors;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SensorTypeRegistry {

    public static final DeferredRegister<SensorType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.SENSOR_TYPE, SettlementsMod.MOD_ID);

//    public static final Supplier<SensorType<NeedFoodSensor>> NEED_FOOD_SENSOR = REGISTRY.register(
//            "need_food",
//            () -> new SensorType<>(NeedFoodSensor::new));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
