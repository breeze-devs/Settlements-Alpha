package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.block.crops.BlueberryCropBlock;
import dev.breezes.settlements.block.crops.CornCropBlock;
import dev.breezes.settlements.block.crops.StrawberryCropBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class BlockRegistry {

    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, SettlementsMod.MOD_ID);

    public static final RegistryObject<Block> SAPPHIRE_ORE = registerBlock("sapphire_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.STONE)
                            .strength(2F)
                            .requiresCorrectToolForDrops(),
                    UniformInt.of(3, 6)
            ));
    public static final RegistryObject<Block> RAW_SAPPHIRE_BLOCK = registerBlock("raw_sapphire_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_IRON_BLOCK)));
    public static final RegistryObject<Block> SAPPHIRE_BLOCK = registerBlock("sapphire_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));

    // Sapphire decorations
    public static final RegistryObject<Block> SAPPHIRE_STAIRS = registerBlock("sapphire_stairs",
            () -> new StairBlock(() -> SAPPHIRE_BLOCK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> SAPPHIRE_SLAB = registerBlock("sapphire_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> SAPPHIRE_BUTTON = registerBlock("sapphire_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE_BUTTON).sound(SoundType.AMETHYST), BlockSetType.IRON, 10, true));
    public static final RegistryObject<Block> SAPPHIRE_PRESSURE_PLATE = registerBlock("sapphire_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST), BlockSetType.IRON));

    public static final RegistryObject<Block> SAPPHIRE_FENCE = registerBlock("sapphire_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> SAPPHIRE_FENCE_GATE = registerBlock("sapphire_fence_gate",
            () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST), SoundEvents.CHAIN_PLACE, SoundEvents.ANVIL_BREAK));
    public static final RegistryObject<Block> SAPPHIRE_WALL = registerBlock("sapphire_wall",
            () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));

    // Sapphire redstone
    public static final RegistryObject<Block> SAPPHIRE_DOOR = registerBlock("sapphire_door",
            () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST).noOcclusion(), BlockSetType.IRON));
    public static final RegistryObject<Block> SAPPHIRE_TRAPDOOR = registerBlock("sapphire_trapdoor",
            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST).noOcclusion(), BlockSetType.IRON));

    // Advanced blocks
    static {
    }

    public static final RegistryObject<StrawberryCropBlock> STRAWBERRY_CROP = REGISTRY.register("strawberry_crop",
            () -> new StrawberryCropBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT).noOcclusion().noCollission()));

    public static final RegistryObject<BlueberryCropBlock> BLUEBERRY_CROP = REGISTRY.register(BlueberryCropBlock.CROP_ID, BlueberryCropBlock::new);

    public static final RegistryObject<CornCropBlock> CORN_CROP = REGISTRY.register("corn_crop",
            () -> new CornCropBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT).noOcclusion().noCollission()));


    // Cat mint flower
    public static final RegistryObject<Block> CAT_MINT = registerBlock("cat_mint",
            // Note that the effect duration here is in seconds, not ticks
            () -> new FlowerBlock(() -> MobEffects.LUCK, 5, BlockBehaviour.Properties.copy(Blocks.DANDELION).noOcclusion().noCollission()));

    public static final RegistryObject<Block> POTTED_CAT_MINT = REGISTRY.register("potted_cat_mint",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, CAT_MINT, BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION).noOcclusion()));

    /**
     * Initializes the block registry using the event bus
     */
    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

    /**
     * Registers the block as well as its associated item
     */
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registeredBlock = REGISTRY.register(name, block);
        registerBlockItem(name, registeredBlock);
        return registeredBlock;
    }

    /**
     * Registers the item associated with the given block
     */
    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ItemRegistry.REGISTRY.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

}
