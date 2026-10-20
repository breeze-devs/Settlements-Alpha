package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementSummary;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.ClientBoundSettlementArrivalPacket;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.SettlementAnnouncementType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementAnnouncementQueueTest {

    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));

    private static final UUID ASHFORD = new UUID(0L, 1L);
    private static final UUID MILLBROOK = new UUID(0L, 2L);

    /**
     * An arbitrary start on the monotonic clock, away from zero so no rule can lean on an unset timestamp.
     */
    private static final long START = 1_000_000L;

    private static final float EPSILON = 0.001F;

    // One title at a time

    @Test
    void title_isStartedOnItsFirstFrameOnly() {
        // Arrange: a flag left set would replay the jingle every frame; one never set would never play it
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.DISCOVERY, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing first = queue.title(OVERWORLD, START);
        SettlementAnnouncementQueue.Showing next = queue.title(OVERWORLD, START + 1);

        // Assert
        assertTrue(first.started());
        assertFalse(next.started());
    }

    @Test
    void title_fadesInFromNothing_holdsAtFull_andFadesOut() {
        // Arrange: a title that popped in at full opacity, or faded the wrong way, fails one of these frames
        SettlementAnnouncementQueue.Timing timing = SettlementAnnouncementQueue.MAIN_TITLE;
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.DISCOVERY, OVERWORLD, ASHFORD), START);
        long fadeOutStart = START + timing.fadeInMillis() + timing.holdMillis();

        // Act
        float atStart = queue.title(OVERWORLD, START).opacity();
        float fadedIn = queue.title(OVERWORLD, START + timing.fadeInMillis()).opacity();
        float endOfHold = queue.title(OVERWORLD, fadeOutStart - 1).opacity();
        float halfFadedOut = queue.title(OVERWORLD, fadeOutStart + timing.fadeOutMillis() / 2).opacity();

        // Assert
        assertEquals(0.0F, atStart, EPSILON);
        assertEquals(1.0F, fadedIn, EPSILON);
        assertEquals(1.0F, endOfHold, EPSILON);
        assertEquals(0.5F, halfFadedOut, EPSILON);
    }

    @Test
    void title_runsOutWhileNotDrawn() {
        // Arrange: a title timed by the frames that draw it would pause under a screen and still be up once it closed
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.DISCOVERY, OVERWORLD, ASHFORD), START);
        queue.title(OVERWORLD, START);

        // Act: no frame draws until the title's whole display time has passed
        SettlementAnnouncementQueue.Showing afterTheScreen =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.MAIN_TITLE.totalMillis());

        // Assert
        assertNull(afterTheScreen);
    }

    @Test
    void title_offeredWhileNotDrawn_startsInFullOnTheFirstFrameThatDraws() {
        // Arrange: a title timed from its offer rather than its first drawn frame would be spent before the screen
        // that hid it closed, and its sound would never play
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.DISCOVERY, OVERWORLD, ASHFORD), START);

        // Act: the first frame that draws comes once the title's whole display time has passed
        SettlementAnnouncementQueue.Showing shown =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.MAIN_TITLE.totalMillis());

        // Assert
        assertTrue(shown.started());
        assertEquals(0L, shown.elapsedMillis());
    }

    @Test
    void title_newerMoment_takesThePlaceOfTheOneShowing_onTheNextFrame() {
        // Arrange: a title that waited for the one showing to end would leave a second rename behind the first's
        // whole display time, and renaming again and again would fall further and further behind
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.RENAMED, OVERWORLD, ASHFORD), START);
        queue.title(OVERWORLD, START);
        long fadedIn = START + SettlementAnnouncementQueue.MAIN_TITLE.fadeInMillis();
        queue.title(OVERWORLD, fadedIn);

        // Act
        queue.offer(arrival(SettlementAnnouncementType.RENAMED, OVERWORLD, MILLBROOK), fadedIn);
        SettlementAnnouncementQueue.Showing next = queue.title(OVERWORLD, fadedIn + 1);

        // Assert
        assertEquals(MILLBROOK, next.arrival().settlementId());
        assertTrue(next.started());
    }

    @Test
    void title_newerMoment_takesThePlaceOfTheOnePending() {
        // Arrange: two titles offered before a frame draws, which a queue keeping both would show one after the other
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.RENAMED, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.RENAMED, OVERWORLD, MILLBROOK), START);

        // Act
        SettlementAnnouncementQueue.Showing shown = queue.title(OVERWORLD, START);
        SettlementAnnouncementQueue.Showing afterIt =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.MAIN_TITLE.totalMillis());

        // Assert
        assertEquals(MILLBROOK, shown.arrival().settlementId());
        assertNull(afterIt);
    }

    // Comings and goings

    @Test
    void entry_showsOnTheNextDrawnFrame_withItsStart() {
        // Arrange: an entry held back like an exit would leave the player inside the settlement before it says so
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing entry = queue.title(OVERWORLD, START);

        // Assert
        assertEquals(SettlementAnnouncementType.ENTRY, entry.arrival().announcementType());
        assertTrue(entry.started());
    }

    @Test
    void exit_waitsOutTheGrace() {
        // Arrange: an exit shown at once, or a grace compared with '>' instead of '>=', fails one of the two frames
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing inGrace =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS - 1);
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertNull(inGrace);
        assertEquals(SettlementAnnouncementType.EXIT, afterGrace.arrival().announcementType());
    }

    @Test
    void steppingOutAndStraightBackIn_showsNothing() {
        // Arrange: a re-entry that left the pending exit in place would show "Leaving" to a player standing inside, and
        // one shown as an entry would ring the bell for a departure the player never saw
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START + 1);

        // Act
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertNull(afterGrace);
    }

    @Test
    void entryAndItsExitBeforeAFrameDraws_showNothing() {
        // Arrange: an exit that replaced the undrawn entry would say "Leaving Ashford" to a player never shown arriving
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing atOnce = queue.title(OVERWORLD, START);
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertNull(atOnce);
        assertNull(afterGrace);
    }

    @Test
    void inOutAndBackInBeforeAFrameDraws_showsTheEntry() {
        // Arrange: an exit that replaced the undrawn entry turns the last entry into a return from a departure never
        // shown, and the player is never shown arriving at all
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing entry = queue.title(OVERWORLD, START);

        // Assert
        assertEquals(SettlementAnnouncementType.ENTRY, entry.arrival().announcementType());
        assertTrue(entry.started());
    }

    @Test
    void exitAfterTheEntryDrew_stillShows() {
        // Arrange: an exit cancelled by any entry into its settlement, drawn or not, would never say "Leaving"
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START);
        queue.title(OVERWORLD, START);

        // Act
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START + 1);
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(OVERWORLD, START + 1 + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertEquals(SettlementAnnouncementType.EXIT, afterGrace.arrival().announcementType());
    }

    @Test
    void walkingIntoANeighbor_showsOnlyTheNeighbor() {
        // Arrange: an exit dropped only by a re-entry into the same settlement would still say "Leaving Ashford"
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, MILLBROOK), START + 1);

        // Act
        SettlementAnnouncementQueue.Showing neighbor = queue.title(OVERWORLD, START + 1);
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertEquals(MILLBROOK, neighbor.arrival().settlementId());
        assertEquals(MILLBROOK, afterGrace.arrival().settlementId());
        assertFalse(afterGrace.started());
    }

    @Test
    void discoveringANeighbor_dropsThePendingExit() {
        // Arrange: a discovery that left the pending exit in place would be cut short by "Leaving Ashford"
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.DISCOVERY, OVERWORLD, MILLBROOK), START + 1);
        queue.title(OVERWORLD, START + 1);

        // Act
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertEquals(SettlementAnnouncementType.DISCOVERY, afterGrace.arrival().announcementType());
        assertFalse(afterGrace.started());
    }

    @Test
    void exit_doesNotDisplaceADiscoveryWaitingToBeShown() {
        // Arrange: a discovery held back by a screen, then the player leaves; an exit that took the waiting title's
        // place would drop the discovery for good, since the server sends a settlement's discovery only once
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.DISCOVERY, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing discovery = queue.title(OVERWORLD, START);
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertEquals(SettlementAnnouncementType.DISCOVERY, discovery.arrival().announcementType());
        assertEquals(SettlementAnnouncementType.DISCOVERY, afterGrace.arrival().announcementType());
    }

    @Test
    void reentry_leavesADiscoveryWaitingToBeShownInPlace() {
        // Arrange: in, out, and back in before a frame draws; an entry that replaced the waiting title would ring the
        // bell for a plain entry, and the player would never see the discovery
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.DISCOVERY, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing shown = queue.title(OVERWORLD, START);

        // Assert
        assertEquals(SettlementAnnouncementType.DISCOVERY, shown.arrival().announcementType());
    }

    @Test
    void returnAfterTheExitShowed_showsTheEntryInItsPlace() {
        // Arrange: a return silenced by a departure the player already saw, rather than only by one still in its
        // grace, would leave "Leaving Ashford" up over a player back inside
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        long shown = START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS;
        queue.title(OVERWORLD, shown);

        // Act
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), shown + 1);
        SettlementAnnouncementQueue.Showing returned = queue.title(OVERWORLD, shown + 1);

        // Assert
        assertEquals(SettlementAnnouncementType.ENTRY, returned.arrival().announcementType());
        assertTrue(returned.started());
    }

    @Test
    void pendingExitFromOneDimensionsSettlement_doesNotSilenceTheSameIdInAnother() {
        // Arrange: ids are unique only within a dimension, so a return matched by id alone swallows this entry
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.EXIT, NETHER, ASHFORD), START);

        // Act
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START + 1);

        // Assert
        assertEquals(OVERWORLD, queue.title(OVERWORLD, START + 1).arrival().dimension());
    }

    @Test
    void disband_showsAtOnce_inPlaceOfAPendingExit() {
        // Arrange: a disband given the exit's grace, or kept behind the exit, is not shown on this frame
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        queue.offer(arrival(SettlementAnnouncementType.DISBANDED, OVERWORLD, MILLBROOK), START + 1);

        // Act
        SettlementAnnouncementQueue.Showing disband = queue.title(OVERWORLD, START + 1);

        // Assert
        assertEquals(SettlementAnnouncementType.DISBANDED, disband.arrival().announcementType());
    }

    @Test
    void exitArrivingAfterALevelChange_neitherShowsNorDisplacesTheNewLevelsEntry() {
        // Arrange: the server posts a dimension change's exit after the client is already in the new level, here
        // after the entry into the Nether settlement showed; an exit taken in regardless would replace that entry once
        // its grace ran out
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.ENTRY, NETHER, MILLBROOK), START);
        queue.title(NETHER, START);

        // Act
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(NETHER, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertEquals(MILLBROOK, afterGrace.arrival().settlementId());
        assertFalse(afterGrace.started());
    }

    @Test
    void exitStillInItsGraceAtALevelChange_isDropped() {
        // Arrange: an exit whose dimension is checked only as it arrives would say "Leaving Ashford" in the Nether
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing inTheNether =
                queue.title(NETHER, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertNull(inTheNether);
    }

    @Test
    void exitTitle_isShorterThanAnEntryTitle() {
        // Arrange: the design calls for a shorter title on leaving; timings that made them equal fail here
        SettlementAnnouncementQueue entering = queue();
        entering.offer(arrival(SettlementAnnouncementType.ENTRY, OVERWORLD, ASHFORD), START);
        entering.title(OVERWORLD, START);
        SettlementAnnouncementQueue leaving = queue();
        leaving.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, ASHFORD), START);
        long shown = START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS;
        leaving.title(OVERWORLD, shown);

        // Act
        long exitTitle = SettlementAnnouncementQueue.EXIT_TITLE.totalMillis();
        SettlementAnnouncementQueue.Showing entry = entering.title(OVERWORLD, START + exitTitle);
        SettlementAnnouncementQueue.Showing exit = leaving.title(OVERWORLD, shown + exitTitle);

        // Assert
        assertNotNull(entry);
        assertNull(exit);
    }

    // The switch and the session

    @ParameterizedTest
    @EnumSource(SettlementAnnouncementType.class)
    void titlesSwitchedOff_showNothing(SettlementAnnouncementType announcementType) {
        // Arrange: a switch that missed one type would still show that type's title and play its sound
        SettlementAnnouncementQueue queue = new SettlementAnnouncementQueue(new SettlementTitleConfig(false));
        queue.offer(arrival(announcementType, OVERWORLD, ASHFORD), START);

        // Act
        SettlementAnnouncementQueue.Showing afterGrace =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertNull(afterGrace);
    }

    @Test
    void sessionEnd_forgetsTheTitleShowing_andThePendingOne() {
        // Arrange: state kept across a disconnect would surface the last world's title or exit in the next world
        SettlementAnnouncementQueue queue = queue();
        queue.offer(arrival(SettlementAnnouncementType.DISCOVERY, OVERWORLD, MILLBROOK), START);
        queue.title(OVERWORLD, START);
        queue.offer(arrival(SettlementAnnouncementType.EXIT, OVERWORLD, MILLBROOK), START);

        // Act
        queue.onClientSessionEnded();
        SettlementAnnouncementQueue.Showing showing = queue.title(OVERWORLD, START + 1);
        SettlementAnnouncementQueue.Showing staleExit =
                queue.title(OVERWORLD, START + SettlementAnnouncementQueue.EXIT_GRACE_MILLIS);

        // Assert
        assertNull(showing);
        assertNull(staleExit);
    }

    private static SettlementAnnouncementQueue queue() {
        return new SettlementAnnouncementQueue(new SettlementTitleConfig(true));
    }

    private static ClientBoundSettlementArrivalPacket arrival(SettlementAnnouncementType announcementType,
                                                              ResourceKey<Level> dimension,
                                                              UUID settlementId) {
        return ClientBoundSettlementArrivalPacket.builder()
                .announcementType(announcementType)
                .dimension(dimension)
                .settlementId(settlementId)
                .name("Filler")
                .status(announcementType == SettlementAnnouncementType.DISCOVERY ? new SettlementSummary.Fading(3, 1, 2) : null)
                .previousName(announcementType == SettlementAnnouncementType.RENAMED ? "Former filler" : null)
                .build();
    }

}
