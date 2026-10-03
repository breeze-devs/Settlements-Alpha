package dev.breezes.settlements.domain.tags;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Holder for all mod-defined damage type tag keys.
 * <p>
 * Tag JSON files live under data/settlements/tags/damage_type/.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementsDamageTypeTags {

    /**
     * Damage types whose hits never register as a threat to a villager.
     */
    public static final TagKey<DamageType> DOES_NOT_ALARM_VILLAGERS = TagKey.create(Registries.DAMAGE_TYPE,
            ResourceLocationUtil.mod("does_not_alarm_villagers"));

}
