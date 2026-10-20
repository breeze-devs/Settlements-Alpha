package dev.breezes.settlements.application.settlement.membership;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;

import java.util.UUID;

/**
 * A villager's reference to the settlement it belongs to. The settlement it names may no longer exist.
 *
 * @param settlementId the settlement's id, unique only within its dimension
 * @param dimension    the settlement's dimension, which need not be the one the villager is in
 */
public record SettlementMembership(UUID settlementId, ResourceKey<Level> dimension) {

    public SettlementMembership {
        Validate.notNull(settlementId, "Membership settlement id must not be null");
        Validate.notNull(dimension, "Membership dimension must not be null for settlement %s", settlementId);
    }

}
