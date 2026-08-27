package dev.breezes.settlements.domain.ai.worldevent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the decoupled {@code forceRemember}/{@code seedWorthy} flag semantics on
 * {@link WorldEventType} constants.
 * <p>
 * Critical invariants:
 * <ol>
 *   <li>The constants enrolled below keep forceRemember == seedWorthy, so
 *       {@link WorldEventType#isSelfRememberableTerminalEvent()} and
 *       {@link WorldEventType#isSeedWorthy()} agree for them.</li>
 *   <li>The sighting constants (ZOMBIE_SIGHTED through GOLEM_SIGHTED) exercise the
 *       decoupling: ZOMBIE_SIGHTED has forceRemember=true AND seedWorthy=true, while the
 *       social sightings have forceRemember=false AND seedWorthy=true.</li>
 * </ol>
 */
class WorldEventTypeTest {

    /**
     * The enrolled constants deliberately keep forceRemember == seedWorthy: they are either
     * salient enough to both bypass the gate and seed a monologue, or routine enough to do
     * neither.
     * <p>
     * The list is explicit rather than derived from the enum because the two flags are genuinely
     * independent — COURTSHIP_DATE_COMPLETED and the social sightings decouple them on purpose and
     * are covered by their own tests. Enrolling a constant here is a statement that its flags are
     * meant to move together.
     */
    @ParameterizedTest
    @EnumSource(value = WorldEventType.class, names = {
            "SHEEP_SHEARED", "SHEEP_DYED", "RESOURCE_HARVESTED", "FARMLAND_CULTIVATED",
            "TRADE_COMPLETED", "COURTSHIP_CHILD_BIRTH", "COURTSHIP_REJECTED",
            "TRADE_INVITE_SENT", "COURTSHIP_INVITE_SENT",
            "COW_MILKED", "FISH_CAUGHT", "STONE_CUT", "RESOURCE_EXCAVATED",
            "MEAT_SMOKED", "ORE_SMELTED", "FURNACE_MISFIRED", "LIVESTOCK_BUTCHERED",
            "ITEM_ENCHANTED", "LEATHER_DYED", "LEATHER_WASHED", "ANIMAL_BRED",
            "ANIMAL_TAMED", "WOLF_WASHED", "WOLF_FED", "DOG_WALKED",
            "GOLEM_REPAIRED", "POTION_THROWN", "BELL_RUNG", "TARGET_EGGED",
            "CHICKENS_CHASED", "CHICKENS_REVENGED", "LANDSCAPE_SURVEYED",
            "CHEST_MANAGED", "ITEM_COLLECTED"
    })
    void alignedConstants_forceRememberAndSeedWorthyAreEqual(WorldEventType type) {
        // Arrange — derived implicitly from the enum constant

        // Act
        boolean forceRemember = type.isSelfRememberableTerminalEvent();
        boolean seedWorthy = type.isSeedWorthy();

        // Assert
        assertEquals(forceRemember, seedWorthy,
                "Constant " + type + " must have matching forceRemember/seedWorthy flags");
    }

    @Test
    void tradeCompleted_isForceRememberAndSeedWorthy() {
        // Arrange
        WorldEventType type = WorldEventType.TRADE_COMPLETED;

        // Act & Assert
        assertTrue(type.isSelfRememberableTerminalEvent(), "TRADE_COMPLETED must force-remember (salient deed)");
        assertTrue(type.isSeedWorthy(), "TRADE_COMPLETED must seed monologue");
    }

    @Test
    void courtshipDateCompleted_seedsMonologueWithoutForceRemembering() {
        // Arrange
        WorldEventType type = WorldEventType.COURTSHIP_DATE_COMPLETED;

        // Act & Assert — the majority courtship outcome must not bypass the importance gate,
        // or routine dates would crowd the bounded knowledge store
        assertFalse(type.isSelfRememberableTerminalEvent(), "COURTSHIP_DATE_COMPLETED must not force-remember (routine outcome)");
        assertTrue(type.isSeedWorthy(), "COURTSHIP_DATE_COMPLETED must seed monologue (better material than a harvest)");
    }

    @Test
    void courtshipDateCompleted_ranksBelowChildBirth() {
        // Arrange, Act
        float dateImportance = WorldEventType.COURTSHIP_DATE_COMPLETED.getBaseImportance();
        float birthImportance = WorldEventType.COURTSHIP_CHILD_BIRTH.getBaseImportance();

        // Assert — a childless date is deliberately less notable than a birth
        assertTrue(dateImportance < birthImportance,
                "COURTSHIP_DATE_COMPLETED must rank below COURTSHIP_CHILD_BIRTH");
    }

    @Test
    void tradeInviteSent_isNeitherForceRememberNorSeedWorthy() {
        // Arrange
        WorldEventType type = WorldEventType.TRADE_INVITE_SENT;

        // Act & Assert
        assertFalse(type.isSelfRememberableTerminalEvent(), "TRADE_INVITE_SENT must not force-remember (transitional step)");
        assertFalse(type.isSeedWorthy(), "TRADE_INVITE_SENT must not seed monologue");
    }

    @Test
    void zombieSighted_isForceRememberAndSeedWorthy() {
        // Arrange
        WorldEventType type = WorldEventType.ZOMBIE_SIGHTED;

        // Act & Assert — threats bypass the importance gate AND surface in monologue
        assertTrue(type.isSelfRememberableTerminalEvent(), "ZOMBIE_SIGHTED must force-remember (threat)");
        assertTrue(type.isSeedWorthy(), "ZOMBIE_SIGHTED must seed monologue");
    }

    @Test
    void playerSighted_isNotForceRememberButIsSeedWorthy() {
        // Arrange
        WorldEventType type = WorldEventType.PLAYER_SIGHTED;

        // Act & Assert — rides the importance gate but can appear in monologue when admitted
        assertFalse(type.isSelfRememberableTerminalEvent(), "PLAYER_SIGHTED must not bypass importance gate");
        assertTrue(type.isSeedWorthy(), "PLAYER_SIGHTED must be seed-worthy so it surfaces in monologue if admitted");
    }

    @Test
    void wanderingTraderSighted_isNotForceRememberButIsSeedWorthy() {
        // Arrange
        WorldEventType type = WorldEventType.WANDERING_TRADER_SIGHTED;

        // Act & Assert
        assertFalse(type.isSelfRememberableTerminalEvent(), "WANDERING_TRADER_SIGHTED must not bypass importance gate");
        assertTrue(type.isSeedWorthy(), "WANDERING_TRADER_SIGHTED must be seed-worthy");
    }

    @Test
    void golemSighted_isNotForceRememberButIsSeedWorthy() {
        // Arrange
        WorldEventType type = WorldEventType.GOLEM_SIGHTED;

        // Act & Assert
        assertFalse(type.isSelfRememberableTerminalEvent(), "GOLEM_SIGHTED must not bypass importance gate");
        assertTrue(type.isSeedWorthy(), "GOLEM_SIGHTED must be seed-worthy");
    }

    @Test
    void sightingConstants_forceRememberAndSeedWorthyAreIndependent() {
        // Arrange — social sightings have forceRemember=false but seedWorthy=true
        WorldEventType[] socialSightings = {
                WorldEventType.PLAYER_SIGHTED,
                WorldEventType.WANDERING_TRADER_SIGHTED,
                WorldEventType.GOLEM_SIGHTED
        };

        for (WorldEventType type : socialSightings) {
            // Act
            boolean forceRemember = type.isSelfRememberableTerminalEvent();
            boolean seedWorthy = type.isSeedWorthy();

            // Assert — these differ, proving the flags are independent
            assertFalse(forceRemember, type + " forceRemember must be false");
            assertTrue(seedWorthy, type + " seedWorthy must be true");
        }
    }

    @ParameterizedTest
    @EnumSource(value = WorldEventType.class, names = {
            "ZOMBIE_SIGHTED", "PLAYER_SIGHTED", "WANDERING_TRADER_SIGHTED", "GOLEM_SIGHTED"
    })
    void sightingConstants_areSelfWitnessed(WorldEventType type) {
        // Every sighting is witnessed first-hand by each perceiver, so it carries no actor and
        // force-remember must reach all witnesses (see PerceptionPipeline).
        assertTrue(type.isSelfWitnessed(), type + " must be self-witnessed (no single doer)");
    }

    @ParameterizedTest
    @EnumSource(value = WorldEventType.class, names = {
            "ITEM_COLLECTED", "RESOURCE_HARVESTED", "TRADE_COMPLETED", "FURNACE_MISFIRED"
    })
    void deedConstants_areNotSelfWitnessed(WorldEventType type) {
        // Deeds have a single doer; force-remember applies to that doer, not to bystanders.
        assertFalse(type.isSelfWitnessed(), type + " must not be self-witnessed (it has a doer)");
    }

}
