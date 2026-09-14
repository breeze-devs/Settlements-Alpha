package dev.breezes.settlements.domain.tags;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Holder for all mod-defined entity type tag keys.
 * <p>
 * Tag JSON files live under {@code data/settlements/tags/entity_type/}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementsEntityTypeTags {

    /**
     * Entities that are allied to the villager.
     */
    public static final TagKey<EntityType<?>> VILLAGER_ALLIES = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocationUtil.mod("villager_allies"));

}
