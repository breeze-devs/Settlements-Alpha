package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ItemRegistry {

    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, SettlementsMod.MOD_ID);

    public static final RegistryObject<Item> BASE_VILLAGER_SPAWN_EGG = REGISTRY.register("base_villager_spawn_egg",
            () -> new ForgeSpawnEggItem(EntityRegistry.BASE_VILLAGER, 0x7E3080, 0x2BD125, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
