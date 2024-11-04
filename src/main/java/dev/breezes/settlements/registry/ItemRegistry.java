package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemRegistry {

    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(BuiltInRegistries.ITEM, SettlementsMod.MOD_ID);

    public static final DeferredHolder<Item, SpawnEggItem> BASE_VILLAGER_SPAWN_EGG = REGISTRY.register("base_villager_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.BASE_VILLAGER, 0x7E3080, 0x2BD125, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
