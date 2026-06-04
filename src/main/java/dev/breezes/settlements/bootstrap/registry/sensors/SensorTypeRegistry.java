package dev.breezes.settlements.bootstrap.registry.sensors;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.minecraft.ai.sensors.OwnedPetsSensor;
import dev.breezes.settlements.infrastructure.minecraft.ai.sensors.SettlementsVillagerBabiesSensor;
import dev.breezes.settlements.infrastructure.minecraft.ai.sensors.VillageChestsSensor;
import dev.breezes.settlements.infrastructure.minecraft.ai.sensors.WillingCourtshipPartnersSensor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class SensorTypeRegistry {

    public static final DeferredRegister<SensorType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.SENSOR_TYPE, SettlementsMod.MOD_ID);

    public static final Supplier<SensorType<OwnedPetsSensor>> OWNED_PETS_SENSOR = REGISTRY.register(
            "owned_pets",
            () -> new SensorType<>(OwnedPetsSensor::new));

    public static final Supplier<SensorType<VillageChestsSensor>> VILLAGE_CHESTS_SENSOR = REGISTRY.register(
            "village_chests_sensor",
            () -> new SensorType<>(VillageChestsSensor::new));

    public static final Supplier<SensorType<SettlementsVillagerBabiesSensor>> SETTLEMENTS_VILLAGER_BABIES_SENSOR = REGISTRY.register(
            "villager_babies_sensor",
            () -> new SensorType<>(SettlementsVillagerBabiesSensor::new));

    public static final Supplier<SensorType<WillingCourtshipPartnersSensor>> WILLING_COURTSHIP_PARTNERS_SENSOR = REGISTRY.register(
            "willing_courtship_partners_sensor",
            () -> new SensorType<>(WillingCourtshipPartnersSensor::new));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
