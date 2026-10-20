package dev.breezes.settlements.domain.tags;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Holder for all mod-defined item tag keys.
 * <p>
 * Tag JSON files live under data/settlements/tags/item/.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementsItemTags {

    /**
     * Items that show the borders of nearby settlements while held in either hand.
     */
    public static final TagKey<Item> SHOWS_SETTLEMENT_BORDERS = TagKey.create(Registries.ITEM,
            ResourceLocationUtil.mod("shows_settlement_borders"));

}
