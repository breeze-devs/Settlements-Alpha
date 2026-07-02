package dev.breezes.settlements.domain.farming;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;

/**
 * Datapack codec for {@link CultivationCropDefinition}. Unlike the other tail codecs this one
 * resolves its ids against live {@link BuiltInRegistries}: {@code seed_item}/{@code display_item}
 * must resolve to a real item and {@code crop_block} to an actual {@link CropBlock}. That registry
 * coupling makes it server-only (the reload runs on the main thread), which is the deliberate
 * fail-fast trade-off for cultivation crops.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CultivationCropDefinitionCodec {

    public static final Codec<CultivationCropDefinition> CODEC = RecordCodecBuilder.<CultivationCropDefinition>create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.fieldOf("seed_item").forGetter(CultivationCropDefinition::seedItem),
                            ResourceLocation.CODEC.fieldOf("display_item").forGetter(CultivationCropDefinition::displayItem),
                            ResourceLocation.CODEC.fieldOf("crop_block").forGetter(CultivationCropDefinition::cropBlock)
                    ).apply(instance, CultivationCropDefinition::new))
            .flatXmap(CultivationCropDefinitionCodec::validate, DataResult::success);

    private static DataResult<CultivationCropDefinition> validate(CultivationCropDefinition definition) {
        DataResult<CultivationCropDefinition> seedError = requireItem(definition.seedItem(), "seed_item", definition);
        if (seedError != null) {
            return seedError;
        }

        DataResult<CultivationCropDefinition> displayError = requireItem(definition.displayItem(), "display_item", definition);
        if (displayError != null) {
            return displayError;
        }

        Block block = BuiltInRegistries.BLOCK.get(definition.cropBlock());
        if (!(block instanceof CropBlock)) {
            return DataResult.error(() -> "crop_block '" + definition.cropBlock() + "' is not a live CropBlock");
        }

        return DataResult.success(definition);
    }

    /**
     * Returns {@code null} when the id resolves to a real item; otherwise a decode error carrying the
     * offending field name, so the whole entry is rejected rather than silently accepting AIR.
     */
    private static DataResult<CultivationCropDefinition> requireItem(ResourceLocation itemId, String fieldName, CultivationCropDefinition definition) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return DataResult.error(() -> "unknown " + fieldName + " '" + itemId + "'");
        }
        return null;
    }

}
