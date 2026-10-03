package dev.breezes.settlements.bootstrap.registry.damagetypes;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Keys for the mod's damage types.
 * <p>
 * Damage types are a datapack registry: each entry is defined by its JSON under
 * data/settlements/damage_type/, so this class holds keys and registers nothing on the event bus.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DamageTypeRegistry {

    public static final ResourceKey<DamageType> CUCCO_PECK = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocationUtil.mod("cucco_peck"));

}
