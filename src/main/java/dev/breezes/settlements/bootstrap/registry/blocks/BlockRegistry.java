package dev.breezes.settlements.bootstrap.registry.blocks;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.minecraft.blocks.DormantOreBlock;
import dev.breezes.settlements.infrastructure.minecraft.blocks.totem.TotemOfCultivationBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockRegistry {

    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK, SettlementsMod.MOD_ID);

    public static final DeferredHolder<Block, DormantOreBlock> DORMANT_ORE = REGISTRY.register("dormant_ore",
            () -> new DormantOreBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
                            .noOcclusion()
                            .randomTicks()
                            .pushReaction(PushReaction.BLOCK),
                    DormantOreBlock.Host.STONE
            ));

    public static final DeferredHolder<Block, DormantOreBlock> DORMANT_DEEPSLATE_ORE = REGISTRY.register("dormant_deepslate_ore",
            () -> new DormantOreBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DEEPSLATE)
                            .requiresCorrectToolForDrops()
                            .strength(3.5F, 6.0F)
                            .sound(SoundType.DEEPSLATE)
                            .noOcclusion()
                            .randomTicks()
                            .pushReaction(PushReaction.BLOCK),
                    DormantOreBlock.Host.DEEPSLATE
            ));

    public static final DeferredHolder<Block, TotemOfCultivationBlock> TOTEM_OF_CULTIVATION = REGISTRY.register("totem_of_cultivation",
            () -> new TotemOfCultivationBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .noOcclusion()
                            .instabreak()
                            .sound(SoundType.WET_GRASS)
                            .pushReaction(PushReaction.DESTROY)
            ));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
