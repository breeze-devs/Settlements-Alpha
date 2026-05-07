package dev.breezes.settlements.domain.ai.catalog;

import dev.breezes.settlements.domain.time.GameTicks;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorUiDisplayInfoTest {

    @Test
    void toPromptLine_formatsHeavyWorkBehavior() {
        // Arrange
        BehaviorPlanningMetadata descriptor = BehaviorPlanningMetadata.builder()
                .key(BehaviorKey.HARVEST_SUGARCANE)
                .displayName("Harvest Sugar Cane")
                .description("Cut mature sugarcane from nearby fields")
                .category(BehaviorCategory.WORK)
                .intensity(WorkIntensity.HEAVY)
                .estimatedDuration(GameTicks.minutes(3))
                .preconditionSummary("Requires harvestable sugarcane nearby")
                .interruptible(false)
                .build();

        // Act
        String promptLine = descriptor.toPromptLine();

        // Assert
        assertEquals("[HEAVY WORK] harvest_sugarcane — Cut mature sugarcane from nearby fields (~3 minutes)", promptLine);
    }

    @Test
    void toPromptLine_formatsSocialBehavior() {
        // Arrange
        BehaviorPlanningMetadata descriptor = BehaviorPlanningMetadata.builder()
                .key(BehaviorKey.of("gossip"))
                .displayName("Gossip")
                .description("Chat with a nearby villager")
                .category(BehaviorCategory.SOCIAL)
                .intensity(WorkIntensity.NONE)
                .estimatedDuration(GameTicks.minutes(2))
                .preconditionSummary("Requires another villager nearby")
                .interruptible(false)
                .build();

        // Act
        String promptLine = descriptor.toPromptLine();

        // Assert
        assertEquals("[SOCIAL] gossip — Chat with a nearby villager (~2 minutes)", promptLine);
    }

    @Test
    void isInterruptible_returnsDeclaredTrueValue() {
        // Arrange
        BehaviorPlanningMetadata descriptor = metadataBuilder(BehaviorKey.MILK_COW)
                .interruptible(true)
                .build();

        // Act / Assert
        assertTrue(descriptor.isInterruptible());
    }

    @Test
    void isInterruptible_defaultsToFalseWhenNotDeclared() {
        // Arrange
        BehaviorPlanningMetadata descriptor = metadataBuilder(BehaviorKey.ENCHANT_ITEM)
                .build();

        // Act / Assert
        assertFalse(descriptor.isInterruptible());
    }

    @Test
    void interruptible_isNotSurfacedInPromptLine() {
        // Arrange
        BehaviorPlanningMetadata descriptor = metadataBuilder(BehaviorKey.MILK_COW)
                .description("Milk available cows")
                .estimatedDuration(GameTicks.minutes(2))
                .interruptible(true)
                .build();

        // Act
        String promptLine = descriptor.toPromptLine();

        // Assert
        assertEquals("[LIGHT WORK] milk_cow — Milk available cows (~2 minutes)", promptLine);
    }

    @Test
    void memoryHintSets_arePopulatedByBuilder() {
        // Arrange
        BehaviorPlanningMetadata descriptor = metadataBuilder(BehaviorKey.HARVEST_SUGARCANE)
                .relevantMemoryHint("sugarcane_ready")
                .producedObservationHint("sugarcane_harvested")
                .build();

        // Act / Assert
        assertEquals(Set.of("sugarcane_ready"), descriptor.getRelevantMemoryHints());
        assertEquals(Set.of("sugarcane_harvested"), descriptor.getProducedObservationHints());
    }

    @Test
    void memoryHints_areNotSurfacedInPromptLine() {
        // Arrange
        BehaviorPlanningMetadata descriptor = metadataBuilder(BehaviorKey.HARVEST_SUGARCANE)
                .description("Cut mature sugarcane from nearby fields")
                .estimatedDuration(GameTicks.minutes(3))
                .relevantMemoryHint("sugarcane_ready")
                .producedObservationHint("sugarcane_harvested")
                .build();

        // Act
        String promptLine = descriptor.toPromptLine();

        // Assert
        assertEquals("[LIGHT WORK] harvest_sugarcane — Cut mature sugarcane from nearby fields (~3 minutes)", promptLine);
    }

    @Nested
    class ChannelCompatibility {

        @Test
        void disjointChannels_areCompatible() {
            // Arrange
            BehaviorPlanningMetadata walkToMarket = descriptorWithChannels(BehaviorKey.of("walk_to_market"),
                    Set.of(BehaviorChannel.MOVEMENT));
            BehaviorPlanningMetadata gossip = descriptorWithChannels(BehaviorKey.of("gossip"),
                    Set.of(BehaviorChannel.COGNITION, BehaviorChannel.SOCIAL));

            // Act / Assert
            assertTrue(walkToMarket.isChannelCompatibleWith(gossip));
            assertTrue(gossip.isChannelCompatibleWith(walkToMarket));
        }

        @Test
        void overlappingChannels_areIncompatible() {
            // Arrange
            BehaviorPlanningMetadata harvest = descriptorWithChannels(BehaviorKey.HARVEST_SUGARCANE,
                    Set.of(BehaviorChannel.MOVEMENT, BehaviorChannel.INTERACTION, BehaviorChannel.COGNITION));
            BehaviorPlanningMetadata gossip = descriptorWithChannels(BehaviorKey.of("gossip"),
                    Set.of(BehaviorChannel.COGNITION, BehaviorChannel.SOCIAL));

            // Act / Assert
            assertFalse(harvest.isChannelCompatibleWith(gossip));
            assertFalse(gossip.isChannelCompatibleWith(harvest));
        }

        @Test
        void emptyChannels_areCompatibleWithEverything() {
            // Arrange
            BehaviorPlanningMetadata pause = descriptorWithChannels(BehaviorKey.of("pause"), Set.of());
            BehaviorPlanningMetadata harvest = descriptorWithChannels(BehaviorKey.HARVEST_SUGARCANE,
                    Set.of(BehaviorChannel.MOVEMENT, BehaviorChannel.INTERACTION, BehaviorChannel.COGNITION));

            // Act / Assert
            assertTrue(pause.isChannelCompatibleWith(harvest));
            assertTrue(harvest.isChannelCompatibleWith(pause));
        }

        @Test
        void bothEmptyChannels_areCompatible() {
            // Arrange
            BehaviorPlanningMetadata pause1 = descriptorWithChannels(BehaviorKey.of("pause_1"), Set.of());
            BehaviorPlanningMetadata pause2 = descriptorWithChannels(BehaviorKey.of("pause_2"), Set.of());

            // Act / Assert
            assertTrue(pause1.isChannelCompatibleWith(pause2));
        }

        @Test
        void routineChoreAndSocial_areCompatible() {
            // Arrange
            BehaviorPlanningMetadata milkCow = descriptorWithChannels(BehaviorKey.MILK_COW,
                    Set.of(BehaviorChannel.MOVEMENT, BehaviorChannel.INTERACTION));
            BehaviorPlanningMetadata gossip = descriptorWithChannels(BehaviorKey.of("gossip"),
                    Set.of(BehaviorChannel.COGNITION, BehaviorChannel.SOCIAL));

            // Act / Assert
            assertTrue(milkCow.isChannelCompatibleWith(gossip));
        }

        @Test
        void focusedWorkAndSocial_areIncompatible() {
            // Arrange
            BehaviorPlanningMetadata enchant = descriptorWithChannels(BehaviorKey.ENCHANT_ITEM,
                    Set.of(BehaviorChannel.INTERACTION, BehaviorChannel.COGNITION));
            BehaviorPlanningMetadata gossip = descriptorWithChannels(BehaviorKey.of("gossip"),
                    Set.of(BehaviorChannel.COGNITION, BehaviorChannel.SOCIAL));

            // Act / Assert
            assertFalse(enchant.isChannelCompatibleWith(gossip));
        }

        @Test
        void identicalChannels_areIncompatible() {
            // Arrange
            BehaviorPlanningMetadata walkToMarket = descriptorWithChannels(BehaviorKey.of("walk_to_market"),
                    Set.of(BehaviorChannel.MOVEMENT));
            BehaviorPlanningMetadata walkToBell = descriptorWithChannels(BehaviorKey.of("walk_to_bell"),
                    Set.of(BehaviorChannel.MOVEMENT));

            // Act / Assert
            assertFalse(walkToMarket.isChannelCompatibleWith(walkToBell));
        }

        @Test
        void eatAndSocial_areCompatible() {
            // Arrange
            BehaviorPlanningMetadata eat = descriptorWithChannels(BehaviorKey.EAT_FOOD,
                    Set.of(BehaviorChannel.INTERACTION));
            BehaviorPlanningMetadata gossip = descriptorWithChannels(BehaviorKey.of("gossip"),
                    Set.of(BehaviorChannel.COGNITION, BehaviorChannel.SOCIAL));

            // Act / Assert
            assertTrue(eat.isChannelCompatibleWith(gossip));
        }

        @Test
        void interruptibleFlag_doesNotAffectChannelCompatibility() {
            // Arrange
            BehaviorPlanningMetadata interruptibleMilkCow = descriptorWithChannels(BehaviorKey.MILK_COW,
                    Set.of(BehaviorChannel.MOVEMENT, BehaviorChannel.INTERACTION), true);
            BehaviorPlanningMetadata uninterruptibleGossip = descriptorWithChannels(BehaviorKey.of("gossip"),
                    Set.of(BehaviorChannel.COGNITION, BehaviorChannel.SOCIAL), false);

            // Act / Assert
            assertTrue(interruptibleMilkCow.isChannelCompatibleWith(uninterruptibleGossip));
        }

        private static BehaviorPlanningMetadata descriptorWithChannels(BehaviorKey key, Set<BehaviorChannel> channels) {
            return descriptorWithChannels(key, channels, false);
        }

        private static BehaviorPlanningMetadata descriptorWithChannels(BehaviorKey key, Set<BehaviorChannel> channels,
                                                                       boolean interruptible) {
            return BehaviorPlanningMetadata.builder()
                    .key(key)
                    .displayName(key.id())
                    .description("test")
                    .category(BehaviorCategory.WORK)
                    .intensity(WorkIntensity.NONE)
                    .requiredChannels(channels)
                    .estimatedDuration(GameTicks.minutes(1))
                    .interruptible(interruptible)
                    .build();
        }

    }

    private static BehaviorPlanningMetadata.BehaviorPlanningMetadataBuilder metadataBuilder(BehaviorKey key) {
        return BehaviorPlanningMetadata.builder()
                .key(key)
                .displayName(key.id())
                .description("Do something useful")
                .category(BehaviorCategory.WORK)
                .intensity(WorkIntensity.LIGHT)
                .estimatedDuration(GameTicks.minutes(1))
                .preconditionSummary("Requires test setup");
    }

}
