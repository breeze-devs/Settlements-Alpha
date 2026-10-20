package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRegionStateTest {

    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));

    private static final UUID ASHFORD = new UUID(0L, 1L);
    private static final UUID MILLBROOK = new UUID(0L, 2L);

    @Test
    void isInSameSettlementAs_isFalse_forTheSameIdInAnotherDimension() {
        // Arrange: generated settlements in two dimensions can share an id
        PlayerRegionState overworldAshford = new PlayerRegionState(OVERWORLD, ASHFORD, null);
        PlayerRegionState netherAshford = new PlayerRegionState(NETHER, ASHFORD, null);

        // Act, Assert
        assertFalse(overworldAshford.isInSameSettlementAs(netherAshford));
        assertFalse(overworldAshford.isInSameBuildingAs(netherAshford));
    }

    @Test
    void isInSameSettlementAs_isTrue_whenOnlyTheBuildingDiffers() {
        // Arrange
        PlayerRegionState inTheSmithy = new PlayerRegionState(OVERWORLD, ASHFORD, "smithy");
        PlayerRegionState onTheStreet = new PlayerRegionState(OVERWORLD, ASHFORD, null);

        // Act, Assert
        assertTrue(inTheSmithy.isInSameSettlementAs(onTheStreet));
        assertFalse(inTheSmithy.isInSameBuildingAs(onTheStreet));
    }

    @Test
    void isInSameBuildingAs_isFalse_forTheSameDefinitionInAnotherSettlement() {
        // Arrange
        PlayerRegionState ashfordSmithy = new PlayerRegionState(OVERWORLD, ASHFORD, "smithy");
        PlayerRegionState millbrookSmithy = new PlayerRegionState(OVERWORLD, MILLBROOK, "smithy");

        // Act, Assert
        assertFalse(ashfordSmithy.isInSameBuildingAs(millbrookSmithy));
    }

}
