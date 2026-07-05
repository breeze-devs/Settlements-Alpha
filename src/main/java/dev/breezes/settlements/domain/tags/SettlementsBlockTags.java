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

    /**
     * Fence gates a villager may path through and operate, mirroring vanilla's own
     * {@code MOB_INTERACTABLE_DOORS}. Raw {@code BlockTags.FENCE_GATES} also drives wall
     * connections, so a packmaker can't exclude a gate from villager operation without breaking
     * the block visually -- this mod-owned tag gives that escape hatch.
     */
    public static final TagKey<Block> MOB_INTERACTABLE_FENCE_GATES = TagKey.create(Registries.BLOCK,
            ResourceLocationUtil.mod("mob_interactable_fence_gates"));

}
