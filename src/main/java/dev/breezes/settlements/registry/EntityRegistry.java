package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.entities.custom.RhinoEntity;
import dev.breezes.settlements.entities.villager.BaseVillager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SettlementsMod.MOD_ID);

    public static final RegistryObject<EntityType<RhinoEntity>> RHINO = REGISTRY.register("rhino",
            () -> EntityType.Builder.of(RhinoEntity::new, MobCategory.CREATURE)
                    .sized(2.5F, 2.5F)
                    .build("rhino"));

    public static final RegistryObject<EntityType<BaseVillager>> BASE_VILLAGER = REGISTRY.register("base_villager",
            () -> EntityType.Builder.<BaseVillager>of(BaseVillager::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("base_villager"));


    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
