package dev.breezes.settlements.domain.tags;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Holder for all mod-defined dimension tag keys.
 * <p>
 * Tag JSON files live under data/settlements/tags/dimension/, and their entries are dimension ids.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementsDimensionTags {

    /**
     * Dimensions in which a bell can found a settlement.
     */
    public static final TagKey<LevelStem> ALLOWS_FOUNDING = TagKey.create(Registries.LEVEL_STEM,
            ResourceLocationUtil.mod("allows_founding"));

}
