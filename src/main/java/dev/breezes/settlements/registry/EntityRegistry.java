package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.wolves.SettlementsWolf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SettlementsMod.MOD_ID);

    public static final Supplier<EntityType<BaseVillager>> BASE_VILLAGER = REGISTRY.register("base_villager",
            () -> EntityType.Builder.of(BaseVillager::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("base_villager"));

    public static final Supplier<EntityType<SettlementsWolf>> SETTLEMENTS_WOLF = REGISTRY.register("settlements_wolf",
            () -> EntityType.Builder.of(SettlementsWolf::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F)
                    .eyeHeight(0.68F)
                    .passengerAttachments(new Vec3(0.0, 0.81875, -0.0625))
                    .clientTrackingRange(10)
                    .build("settlements_wolf"));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
