package dev.breezes.settlements.bootstrap.registry.items;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
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
            () -> new DeferredSpawnEggItem(EntityRegistry.BASE_VILLAGER, 0x6B4030, 0xD49575, new Item.Properties()));

    public static final DeferredHolder<Item, Item> VILLAGER_FISHING_ROD = REGISTRY.register("villager_fishing_rod",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
