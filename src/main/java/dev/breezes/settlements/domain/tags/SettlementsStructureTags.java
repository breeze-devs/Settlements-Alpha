package dev.breezes.settlements.domain.tags;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Holder for all mod-defined structure tag keys.
 * <p>
 * Tag JSON files live under data/settlements/tags/worldgen/structure/.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementsStructureTags {

    /**
     * Structures that are villages. A bell inside one founds an established settlement, since the village stood
     * before anyone recorded it.
     */
    public static final TagKey<Structure> VILLAGES = TagKey.create(Registries.STRUCTURE,
            ResourceLocationUtil.mod("villages"));

}
