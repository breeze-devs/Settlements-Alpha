package dev.breezes.settlements.registry;

import com.tterrag.registrate.util.entry.ItemEntry;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.item.MetalDestroyerItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static dev.breezes.settlements.SettlementsMod.REGISTRATE;

public final class ItemRegistry {

    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, SettlementsMod.MOD_ID);

    public static final RegistryObject<Item> SAPPHIRE = REGISTRY.register("sapphire", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_SAPPHIRE = REGISTRY.register("raw_sapphire", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> METAL_DETECTOR = REGISTRY.register("metal_destroyer", () -> new MetalDestroyerItem(
            new Item.Properties().durability(30)));

    public static final ItemEntry<Item> SAPPHIRE_STAFF = REGISTRATE.get().item("sapphire_staff", Item::new)
            .properties(properties -> properties.stacksTo(1))
            .register();
    public static final ItemEntry<ArmorItem> SAPPHIRE_HELMET = REGISTRATE.get().item("sapphire_helmet", properties -> new ArmorItem(ArmorMaterialRegistry.SAPPHIRE, ArmorItem.Type.HELMET, properties))
            .tag(ItemTags.TRIMMABLE_ARMOR)
            .defaultModel()
            .register();
    public static final ItemEntry<ArmorItem> SAPPHIRE_CHESTPLATE = REGISTRATE.get().item("sapphire_chestplate", properties -> new ArmorItem(ArmorMaterialRegistry.SAPPHIRE, ArmorItem.Type.CHESTPLATE, properties))
            .tag(ItemTags.TRIMMABLE_ARMOR)
            .defaultModel()
            .register();
    public static final ItemEntry<ArmorItem> SAPPHIRE_LEGGINGS = REGISTRATE.get().item("sapphire_leggings", properties -> new ArmorItem(ArmorMaterialRegistry.SAPPHIRE, ArmorItem.Type.LEGGINGS, properties))
            .tag(ItemTags.TRIMMABLE_ARMOR)
            .defaultModel()
            .register();
    public static final ItemEntry<ArmorItem> SAPPHIRE_BOOTS = REGISTRATE.get().item("sapphire_boots", properties -> new ArmorItem(ArmorMaterialRegistry.SAPPHIRE, ArmorItem.Type.BOOTS, properties))
            .tag(ItemTags.TRIMMABLE_ARMOR)
            .register();

    public static final ItemEntry<Item> STRAWBERRY = REGISTRATE.get().item("strawberry", Item::new)
            .properties(properties -> properties.food(FoodRegistry.STRAWBERRY))
            .defaultModel()
            .register();
    public static final ItemEntry<ItemNameBlockItem> STRAWBERRY_SEEDS = REGISTRATE.get().item("strawberry_seeds", properties -> new ItemNameBlockItem(BlockRegistry.STRAWBERRY_CROP.get(), properties))
            .defaultModel()
            .register();

    public static final RegistryObject<Item> CORN_SEEDS = REGISTRY.register("corn_seeds", () -> new ItemNameBlockItem(BlockRegistry.CORN_CROP.get(), new Item.Properties()));
    public static final RegistryObject<Item> CORN = REGISTRY.register("corn", () -> new Item(new Item.Properties().food(FoodRegistry.CORN)));

    public static final RegistryObject<Item> BLUEBERRY_SEEDS = REGISTRY.register("blueberry_seeds", () -> new ItemNameBlockItem(BlockRegistry.BLUEBERRY_CROP.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLUEBERRY = REGISTRY.register("blueberry", () -> new Item(new Item.Properties().food(FoodRegistry.STRAWBERRY)));

    public static final RegistryObject<Item> RHINO_SPAWN_EGG = REGISTRY.register("rhino_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistry.RHINO, 0x7E9680, 0xC5D1C5, new Item.Properties()));
    public static final RegistryObject<Item> BASE_VILLAGER_SPAWN_EGG = REGISTRY.register("base_villager_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistry.BASE_VILLAGER, 0x7E3080, 0x2BD125, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
