package dev.breezes.settlements.infrastructure.minecraft.attachments;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementDirectorySyncHandlerTest {

    private static final UUID ASHFORD = new UUID(0L, 1L);
    private static final UUID BRAMBLE = new UUID(0L, 2L);
    private static final UUID CRESTON = new UUID(0L, 3L);

    private static final SettlementSummary HEALTHY_ASHFORD = new SettlementSummary("Ashford", null);
    private static final SettlementSummary FADING_ASHFORD =
            new SettlementSummary("Ashford", new SettlementSummary.Fading(2, 1, 2));
    private static final SettlementSummary BRAMBLE_SUMMARY = new SettlementSummary("Bramble", null);
    private static final SettlementSummary CRESTON_SUMMARY =
            new SettlementSummary("Creston", new SettlementSummary.Fading(0, 0, 2));

    private final SettlementDirectorySyncHandler handler = new SettlementDirectorySyncHandler();

    @Test
    void initialSync_givesAClientTheWholeDirectory() {
        // Arrange
        SettlementDirectory server = new SettlementDirectory(true,
                Map.of(ASHFORD, FADING_ASHFORD, CRESTON, CRESTON_SUMMARY));

        // Act
        SettlementDirectory client = this.send(server, true, null);

        // Assert
        assertTrue(client.foundingAllowed());
        assertEquals(Map.of(ASHFORD, FADING_ASHFORD, CRESTON, CRESTON_SUMMARY), client.summaries());
    }

    @Test
    void initialSync_replacesTheCopyAClientAlreadyHolds() {
        // Arrange: a respawn in the same dimension keeps the client's level, and its stale copy with it
        SettlementDirectory stale = new SettlementDirectory(true, Map.of(BRAMBLE, BRAMBLE_SUMMARY));
        SettlementDirectory server = new SettlementDirectory(false, Map.of(ASHFORD, HEALTHY_ASHFORD));

        // Act
        SettlementDirectory client = this.send(server, true, stale);

        // Assert
        assertFalse(client.foundingAllowed());
        assertEquals(Map.of(ASHFORD, HEALTHY_ASHFORD), client.summaries());
    }

    @Test
    void initialSync_leavesTheChangesOwedToOtherClientsForTheNextBroadcast() {
        // Arrange
        SettlementDirectory server = new SettlementDirectory(true, Map.of(ASHFORD, HEALTHY_ASHFORD));
        SettlementDirectory earlier = this.send(server, true, null);
        server.put(ASHFORD, FADING_ASHFORD);
        this.send(server, true, null);

        // Act
        SettlementDirectory updated = this.send(server, false, earlier);

        // Assert
        assertEquals(Map.of(ASHFORD, FADING_ASHFORD), updated.summaries());
    }

    @Test
    void broadcast_bringsAClientCopyUpToDate() {
        // Arrange
        SettlementDirectory server = new SettlementDirectory(false,
                Map.of(ASHFORD, HEALTHY_ASHFORD, BRAMBLE, BRAMBLE_SUMMARY));
        SettlementDirectory client = this.send(server, true, null);
        server.put(ASHFORD, FADING_ASHFORD);
        server.remove(BRAMBLE);
        server.put(CRESTON, CRESTON_SUMMARY);

        // Act
        SettlementDirectory updated = this.send(server, false, client);

        // Assert
        assertFalse(updated.foundingAllowed());
        assertEquals(Map.of(ASHFORD, FADING_ASHFORD, CRESTON, CRESTON_SUMMARY), updated.summaries());
    }

    @Test
    void broadcast_carriesOnlyTheSummariesThatChanged() {
        // Arrange
        SettlementDirectory server = new SettlementDirectory(true,
                Map.of(ASHFORD, HEALTHY_ASHFORD, BRAMBLE, BRAMBLE_SUMMARY));
        server.put(CRESTON, CRESTON_SUMMARY);

        // Act: applied to no copy, so whatever the broadcast carried is all the result holds
        SettlementDirectory carried = this.send(server, false, null);

        // Assert
        assertEquals(Map.of(CRESTON, CRESTON_SUMMARY), carried.summaries());
    }

    @Test
    void broadcast_carriesNothing_forASummaryEqualToTheOneHeld() {
        // Arrange: a check-in, or a fading step that keeps the days left, projects the summary already held
        SettlementDirectory server = new SettlementDirectory(true, Map.of(ASHFORD, FADING_ASHFORD));
        boolean changed = server.put(ASHFORD, new SettlementSummary("Ashford", new SettlementSummary.Fading(2, 1, 2)));

        // Act
        SettlementDirectory carried = this.send(server, false, null);

        // Assert
        assertFalse(changed);
        assertTrue(carried.summaries().isEmpty());
    }

    @Test
    void broadcast_carriesEachChangeOnce() {
        // Arrange
        SettlementDirectory server = new SettlementDirectory(true, Map.of(ASHFORD, HEALTHY_ASHFORD));
        server.put(ASHFORD, FADING_ASHFORD);
        this.send(server, false, null);

        // Act
        SettlementDirectory carried = this.send(server, false, null);

        // Assert
        assertTrue(carried.summaries().isEmpty());
    }

    @Test
    void broadcast_changesNothing_onACopyWhoseInitialSyncAlreadyHeldIt() {
        // Arrange: changes made while the level had no players are broadcast later, to a player who joined since
        SettlementDirectory server = new SettlementDirectory(true,
                Map.of(ASHFORD, HEALTHY_ASHFORD, BRAMBLE, BRAMBLE_SUMMARY));
        server.put(ASHFORD, FADING_ASHFORD);
        server.remove(BRAMBLE);
        SettlementDirectory joined = this.send(server, true, null);

        // Act
        SettlementDirectory updated = this.send(server, false, joined);

        // Assert
        assertEquals(Map.of(ASHFORD, FADING_ASHFORD), updated.summaries());
    }

    private SettlementDirectory send(SettlementDirectory server, boolean initialSync,
                                     @Nullable SettlementDirectory previous) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY,
                ConnectionType.NEOFORGE);
        this.handler.write(buffer, server, initialSync);
        // The handler never reads its holder
        return this.handler.read(null, buffer, previous);
    }

}
