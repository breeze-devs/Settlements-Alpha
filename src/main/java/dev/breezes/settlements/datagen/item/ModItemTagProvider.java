package dev.breezes.settlements.datagen.item;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {

    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper helper) {
        super(output, lookupProvider, blockTags, SettlementsMod.MOD_ID, helper);
    }

    @Override
    protected void addTags(@Nonnull HolderLookup.Provider provider) {

    }

}
