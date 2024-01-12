package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ItemTagRegistry {

    public static final TagKey<Item> WIGGLE = createTag("wiggle");

    private static TagKey<Item> createTag(String name) {
        return ItemTags.create(new ResourceLocation(SettlementsMod.MOD_ID, name));
    }

}
