package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class BlockTagRegistry {

    public static final TagKey<Block> WIGGLES = createTag("wiggles");

    private static TagKey<Block> createTag(String name) {
        return BlockTags.create(new ResourceLocation(SettlementsMod.MOD_ID, name));
    }

}
