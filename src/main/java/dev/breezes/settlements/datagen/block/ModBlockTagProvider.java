package dev.breezes.settlements.datagen.block;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.registry.BlockRegistry;
import dev.breezes.settlements.registry.BlockTagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper helper) {
        super(output, lookupProvider, SettlementsMod.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Tool tier tags
//        this.tag(BlockTags.NEEDS_STONE_TOOL)
//                .add(
//                        BlockRegistry.BOUNCE_BLOCK.get()
//                );

        this.tag(BlockTags.NEEDS_IRON_TOOL)
                .add(
                        BlockRegistry.SAPPHIRE_BLOCK.get(),
                        BlockRegistry.SAPPHIRE_ORE.get()
                );

        this.tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(
                        BlockRegistry.RAW_SAPPHIRE_BLOCK.get()
                );

//        this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL)
//                .add(
//                        BlockRegistry.RAW_SAPPHIRE_BLOCK.get()
//                );

        // Tool type tags
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(
                        BlockRegistry.SAPPHIRE_BLOCK.get(),
                        BlockRegistry.RAW_SAPPHIRE_BLOCK.get(),
                        BlockRegistry.SAPPHIRE_ORE.get()
//                        BlockRegistry.BOUNCE_BLOCK.get()
                );

        // Fence tags
        addFenceTags();

        // Custom mod tags
        addCustomTags();
    }

    private void addFenceTags() {
        this.tag(BlockTags.FENCES)
                .add(
                        BlockRegistry.SAPPHIRE_FENCE.get()
                );
        this.tag(BlockTags.FENCE_GATES)
                .add(
                        BlockRegistry.SAPPHIRE_FENCE_GATE.get()
                );
        this.tag(BlockTags.WALLS)
                .add(
                        BlockRegistry.SAPPHIRE_WALL.get()
                );
    }

    private void addCustomTags() {
        this.tag(BlockTagRegistry.WIGGLES)
                .add(
                        BlockRegistry.SAPPHIRE_BLOCK.get()
                )
                .addTag(Tags.Blocks.ORES);
    }

}
