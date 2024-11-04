package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.entities.villager.BaseVillager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SettlementsMod.MOD_ID);

    public static final Supplier<EntityType<BaseVillager>> BASE_VILLAGER = REGISTRY.register("base_villager",
            () -> EntityType.Builder.<BaseVillager>of(BaseVillager::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("base_villager"));


    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
