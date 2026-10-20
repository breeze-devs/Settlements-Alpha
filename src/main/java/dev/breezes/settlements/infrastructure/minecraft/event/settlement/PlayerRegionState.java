package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * The settlement and the building a player was last found in.
 *
 * @param dimension            the settlement's dimension, or null exactly when there is no settlement
 * @param settlementId         the settlement whose claim held the player, or null when none did
 * @param buildingDefinitionId the definition of the settlement's building that held the player, or null when none
 *                             did; never present without a settlement
 */
public record PlayerRegionState(
        @Nullable ResourceKey<Level> dimension,
        @Nullable UUID settlementId,
        @Nullable String buildingDefinitionId
) {

    /**
     * A player outside every settlement.
     */
    public static final PlayerRegionState OUTSIDE = new PlayerRegionState(null, null, null);

    public PlayerRegionState {
        Validate.isTrue((dimension == null) == (settlementId == null),
                "A player region names a dimension exactly when it names a settlement: %s in %s", settlementId,
                dimension);
        Validate.isTrue(buildingDefinitionId == null || settlementId != null,
                "A player region names building %s without a settlement", buildingDefinitionId);
    }

    /**
     * Returns whether both regions hold the same settlement, or both hold none.
     */
    public boolean isInSameSettlementAs(@Nonnull PlayerRegionState other) {
        return Objects.equals(this.dimension, other.dimension) && Objects.equals(this.settlementId, other.settlementId);
    }

    /**
     * Returns whether both regions hold the same settlement, and within it the same building or none.
     */
    public boolean isInSameBuildingAs(@Nonnull PlayerRegionState other) {
        return this.isInSameSettlementAs(other)
                && Objects.equals(this.buildingDefinitionId, other.buildingDefinitionId);
    }

}
