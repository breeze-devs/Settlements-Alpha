package dev.breezes.settlements.infrastructure.minecraft.persistence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.breezes.settlements.domain.generation.history.HistoryEvent;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementFading;
import dev.breezes.settlements.domain.settlement.model.SettlementFounding;
import dev.breezes.settlements.domain.settlement.model.SettlementGatherings;
import dev.breezes.settlements.domain.settlement.model.SettlementLog;
import dev.breezes.settlements.domain.settlement.model.SettlementLogEntry;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.settlement.model.SettlementRoster;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.infrastructure.minecraft.persistence.SettlementSavedDataCodec.StoredState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.firstMember;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.secondMember;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.settlement;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementSavedDataCodecTest {

    private static final UUID ASHFORD = new UUID(0L, 1L);
    private static final UUID MILLBROOK = new UUID(0L, 2L);
    private static final UUID DISBANDED = new UUID(0L, 3L);

    @Test
    void stateCodec_roundTripsAFullyPopulatedSettlement() {
        // Arrange
        List<Settlement> state = List.of(settlement(ASHFORD).build());

        // Act
        List<Settlement> decoded = decode(encode(state));

        // Assert
        assertEquals(state, decoded);
    }

    @Test
    void stateCodec_roundTripsAFreshlyFoundedSettlement() {
        // Arrange — the shape a settlement is stored in when founded: nobody on its roster, nothing logged, no
        // gathering yet, and not fading
        Settlement founded = settlement(ASHFORD)
                .roster(SettlementRoster.EMPTY)
                .log(SettlementLog.EMPTY)
                .gatherings(SettlementGatherings.NONE)
                .fading(null)
                .build();

        // Act
        List<Settlement> decoded = decode(encode(List.of(founded)));

        // Assert
        assertEquals(List.of(founded), decoded);
    }

    @ParameterizedTest
    @EnumSource(SettlementOrigin.class)
    void stateCodec_roundTripsEachOrigin(SettlementOrigin origin) {
        // Arrange
        List<Settlement> state = List.of(settlement(ASHFORD).origin(origin).build());

        // Act
        List<Settlement> decoded = decode(encode(state));

        // Assert
        assertEquals(state, decoded);
    }

    @Test
    void stateCodec_dropsTheSettlement_whoseOriginIsUnknown() {
        // Arrange — the first of two settlements names an origin that does not exist
        Settlement millbrook = settlement(MILLBROOK).name("Millbrook").build();
        JsonElement payload = encode(List.of(settlement(ASHFORD).build(), millbrook));
        settlementAt(payload, 0).addProperty("origin", "NOT_AN_ORIGIN");

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert — no origin is guessed for it
        assertEquals(List.of(millbrook), decoded);
    }

    @Test
    void stateCodec_roundTripsAFoundingThatPredatesTheWorld() {
        // Arrange — founded before day time 0, with its history dated between the founding and the record
        Settlement predating = settlement(ASHFORD)
                .founding(new SettlementFounding(new CivilDateTime(-1_000_000L), new CivilDateTime(24_000L)))
                .history(List.of(new HistoryEvent("settlements:settlement_events/flood", new CivilDateTime(-500_000L))))
                .build();

        // Act
        List<Settlement> decoded = decode(encode(List.of(predating)));

        // Assert
        assertEquals(List.of(predating), decoded);
    }

    @Test
    void stateCodec_dropsOnlyTheUnreadableRosterEntry() {
        // Arrange — the second roster entry loses its member id
        Settlement.SettlementBuilder builder = settlement(ASHFORD)
                .roster(new SettlementRoster(List.of(firstMember(), secondMember())));
        JsonElement payload = encode(List.of(builder.build()));
        rosterEntry(payload, 0, 1).remove("member_id");
        Settlement expected = builder.roster(new SettlementRoster(List.of(firstMember()))).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert — the rest of the roster and the rest of the settlement load as they were
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_dropsOnlyTheRosterEntryItsTypeRejects() {
        // Arrange — the first roster entry decodes to a negative gathering count
        Settlement.SettlementBuilder builder = settlement(ASHFORD)
                .roster(new SettlementRoster(List.of(firstMember(), secondMember())));
        JsonElement payload = encode(List.of(builder.build()));
        rosterEntry(payload, 0, 0).addProperty("gathering_mark", -1);
        Settlement expected = builder.roster(new SettlementRoster(List.of(secondMember()))).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_keepsTheFirstOfTwoRosterEntriesForOneMember() {
        // Arrange — the second roster entry is rewritten to name the first entry's member
        Settlement.SettlementBuilder builder = settlement(ASHFORD)
                .roster(new SettlementRoster(List.of(firstMember(), secondMember())));
        JsonElement payload = encode(List.of(builder.build()));
        JsonObject firstEntry = rosterEntry(payload, 0, 0);
        rosterEntry(payload, 0, 1).add("member_id", firstEntry.get("member_id").deepCopy());
        Settlement expected = builder.roster(new SettlementRoster(List.of(firstMember()))).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert — the settlement survives the repeat instead of being rejected whole
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_dropsOnlyTheUnreadableLogEntry() {
        // Arrange — the second log entry names a type that does not exist
        Settlement.SettlementBuilder builder = settlement(ASHFORD);
        Settlement original = builder.build();
        JsonElement payload = encode(List.of(original));
        logEntry(payload, 0, 1).addProperty("type", "NOT_A_LOG_TYPE");
        List<SettlementLogEntry> survivingEntries = new ArrayList<>(original.log().entries());
        survivingEntries.remove(1);
        Settlement expected = builder.log(new SettlementLog(survivingEntries)).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_dropsOnlyTheHistoryEventItsTypeRejects() {
        // Arrange — the first history event's id is blanked
        HistoryEvent founding = new HistoryEvent("settlements:settlement_events/founded_by_fishermen",
                new CivilDateTime(-480_000L));
        HistoryEvent flood = new HistoryEvent("settlements:settlement_events/flood", new CivilDateTime(-120_000L));
        Settlement.SettlementBuilder builder = settlement(ASHFORD).history(List.of(founding, flood));
        JsonElement payload = encode(List.of(builder.build()));
        settlementAt(payload, 0).getAsJsonArray("history").get(0).getAsJsonObject().addProperty("event_id", "");
        Settlement expected = builder.history(List.of(flood)).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert — the settlement keeps the rest of its history instead of being dropped whole
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_clearsGatheringsTheSettlementRejects_keepingTheSettlement() {
        // Arrange — a gathering count whose last gathering day has gone missing
        Settlement.SettlementBuilder builder = settlement(ASHFORD).gatherings(new SettlementGatherings(4, 17L));
        JsonElement payload = encode(List.of(builder.build()));
        settlementAt(payload, 0).getAsJsonObject("gatherings").remove("last_day");
        Settlement expected = builder.gatherings(SettlementGatherings.NONE).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_readsUnreadableGatheringsAsNone_keepingTheSettlement() {
        // Arrange — a gathering count that is not a number
        Settlement.SettlementBuilder builder = settlement(ASHFORD).gatherings(new SettlementGatherings(4, 17L));
        JsonElement payload = encode(List.of(builder.build()));
        settlementAt(payload, 0).getAsJsonObject("gatherings").addProperty("count", "four");
        Settlement expected = builder.gatherings(SettlementGatherings.NONE).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert — the settlement keeps its roster and log instead of being dropped whole
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_readsARosterThatIsNotAListAsEmpty_keepingTheSettlement() {
        // Arrange
        Settlement.SettlementBuilder builder = settlement(ASHFORD);
        JsonElement payload = encode(List.of(builder.build()));
        settlementAt(payload, 0).addProperty("roster", 5);
        Settlement expected = builder.roster(SettlementRoster.EMPTY).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_roundTripsAFadingThatHasJustBegun() {
        // Arrange — no time spent fading yet, which still reads as fading
        List<Settlement> state = List.of(settlement(ASHFORD).fading(SettlementFading.BEGUN).build());

        // Act
        List<Settlement> decoded = decode(encode(state));

        // Assert
        assertEquals(state, decoded);
    }

    @Test
    void stateCodec_readsAFadingTimeItsTypeRejectsAsNotFading_keepingTheSettlement() {
        // Arrange — a negative time spent fading
        Settlement.SettlementBuilder builder = settlement(ASHFORD).fading(new SettlementFading(ClockTicks.of(1_200L)));
        JsonElement payload = encode(List.of(builder.build()));
        settlementAt(payload, 0).addProperty("fading_ticks_elapsed", -1L);
        Settlement expected = builder.fading(null).build();

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert
        assertEquals(List.of(expected), decoded);
    }

    @Test
    void stateCodec_roundTripsTombstones() {
        // Arrange
        StoredState state = new StoredState(List.of(settlement(ASHFORD).build()), List.of(DISBANDED, MILLBROOK));

        // Act
        StoredState decoded = decodeState(encode(state));

        // Assert
        assertEquals(state, decoded);
    }

    @Test
    void stateCodec_readsAStoreSavedWithoutTombstones() {
        // Arrange — the form of every store saved before a generated settlement disbanded
        Settlement ashford = settlement(ASHFORD).build();
        JsonElement payload = encode(new StoredState(List.of(ashford), List.of(DISBANDED)));
        payload.getAsJsonObject().remove("tombstones");

        // Act
        StoredState decoded = decodeState(payload);

        // Assert
        assertEquals(new StoredState(List.of(ashford), List.of()), decoded);
    }

    @Test
    void stateCodec_dropsOnlyTheUnreadableTombstone() {
        // Arrange — the first of two tombstones loses its id
        Settlement ashford = settlement(ASHFORD).build();
        JsonElement payload = encode(new StoredState(List.of(ashford), List.of(DISBANDED, MILLBROOK)));
        payload.getAsJsonObject().getAsJsonArray("tombstones").get(0).getAsJsonObject().remove("id");

        // Act
        StoredState decoded = decodeState(payload);

        // Assert
        assertEquals(new StoredState(List.of(ashford), List.of(MILLBROOK)), decoded);
    }

    @Test
    void stateCodec_readsTombstonesThatAreNotAListAsNone_keepingTheSettlements() {
        // Arrange
        Settlement ashford = settlement(ASHFORD).build();
        JsonElement payload = encode(new StoredState(List.of(ashford), List.of(DISBANDED)));
        payload.getAsJsonObject().addProperty("tombstones", 5);

        // Act
        StoredState decoded = decodeState(payload);

        // Assert
        assertEquals(new StoredState(List.of(ashford), List.of()), decoded);
    }

    @Test
    void stateCodec_dropsOnlyTheUnreadableSettlement() {
        // Arrange — the first of two settlements loses its name
        Settlement millbrook = settlement(MILLBROOK).name("Millbrook").build();
        JsonElement payload = encode(List.of(settlement(ASHFORD).build(), millbrook));
        settlementAt(payload, 0).remove("name");

        // Act
        List<Settlement> decoded = decode(payload);

        // Assert
        assertEquals(List.of(millbrook), decoded);
    }

    /**
     * The stored form of the given settlements, with no tombstones.
     */
    private static JsonElement encode(List<Settlement> settlements) {
        return encode(new StoredState(settlements, List.of()));
    }

    private static JsonElement encode(StoredState state) {
        return SettlementSavedDataCodec.STATE_CODEC.encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow(message -> new AssertionError("Expected the settlement store to encode: " + message));
    }

    /**
     * The settlements the stored form decodes to.
     */
    private static List<Settlement> decode(JsonElement payload) {
        return decodeState(payload).settlements();
    }

    private static StoredState decodeState(JsonElement payload) {
        return SettlementSavedDataCodec.STATE_CODEC.parse(JsonOps.INSTANCE, payload)
                .getOrThrow(message -> new AssertionError("Expected the settlement store to decode: " + message));
    }

    private static JsonObject settlementAt(JsonElement payload, int index) {
        return payload.getAsJsonObject().getAsJsonArray("settlements").get(index).getAsJsonObject();
    }

    private static JsonObject rosterEntry(JsonElement payload, int settlementIndex, int entryIndex) {
        return settlementAt(payload, settlementIndex).getAsJsonArray("roster").get(entryIndex).getAsJsonObject();
    }

    private static JsonObject logEntry(JsonElement payload, int settlementIndex, int entryIndex) {
        return settlementAt(payload, settlementIndex).getAsJsonArray("log").get(entryIndex).getAsJsonObject();
    }

}
