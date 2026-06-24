package dev.breezes.settlements.domain.tags;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Holder for all mod-defined block tag keys.
 * <p>
 * Tag JSON files live under {@code data/settlements/tags/block/}.
 * Use these constants anywhere a {@link TagKey} or {@code state.is(...)} call is needed
 * so the resource-location strings are never duplicated across call sites.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementsBlockTags {

    /**
     * Blocks that a hoe converts to farmland, e.g. dirt, grass block
     */
    public static final TagKey<Block> TILLABLE = TagKey.create(Registries.BLOCK,
            ResourceLocationUtil.mod("tillable"));

    /**
     * Canopy plants that a villager may safely clear before tilling or planting
     */
    public static final TagKey<Block> TILLABLE_FOLIAGE = TagKey.create(Registries.BLOCK,
            ResourceLocationUtil.mod("tillable_foliage"));

}
