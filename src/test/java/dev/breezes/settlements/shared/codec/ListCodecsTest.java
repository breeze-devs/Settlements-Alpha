package dev.breezes.settlements.shared.codec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListCodecsTest {

    private record PositiveCount(int value) {

        PositiveCount {
            Validate.isTrue(value > 0, "count must be positive: %d", value);
        }

    }

    private record Tally(List<PositiveCount> counts) {
    }

    private static final Codec<PositiveCount> POSITIVE_COUNT_CODEC =
            Codec.INT.xmap(PositiveCount::new, PositiveCount::value);

    private static final Codec<List<PositiveCount>> CODEC = ListCodecs.lenient(POSITIVE_COUNT_CODEC, "positive count");

    @Test
    void lenient_dropsOnlyTheElementThatFailsToDecode() {
        // Arrange — the middle element is not a number at all
        JsonArray payload = new JsonArray();
        payload.add(1);
        payload.add("not a number");
        payload.add(3);

        // Act
        List<PositiveCount> decoded = decode(payload);

        // Assert
        assertEquals(List.of(new PositiveCount(1), new PositiveCount(3)), decoded);
    }

    @Test
    void lenient_dropsOnlyTheElementItsTypeRejects() {
        // Arrange — the middle element decodes as a number its type's constructor refuses
        JsonArray payload = new JsonArray();
        payload.add(1);
        payload.add(-2);
        payload.add(3);

        // Act
        List<PositiveCount> decoded = decode(payload);

        // Assert
        assertEquals(List.of(new PositiveCount(1), new PositiveCount(3)), decoded);
    }

    @Test
    void lenient_keepsAnElementThatDecodesOnlyInPart() {
        // Arrange — the first tally holds a strict list that loses one of its counts
        Codec<Tally> tallyCodec = RecordCodecBuilder.create(instance -> instance.group(
                POSITIVE_COUNT_CODEC.listOf().fieldOf("counts").forGetter(Tally::counts)
        ).apply(instance, Tally::new));
        JsonArray firstCounts = new JsonArray();
        firstCounts.add(1);
        firstCounts.add("not a number");
        JsonArray secondCounts = new JsonArray();
        secondCounts.add(2);
        JsonArray payload = new JsonArray();
        payload.add(tally(firstCounts));
        payload.add(tally(secondCounts));

        // Act
        List<Tally> decoded = ListCodecs.lenient(tallyCodec, "tally").parse(JsonOps.INSTANCE, payload)
                .getOrThrow(message -> new AssertionError("Expected a lenient decode to succeed: " + message));

        // Assert — the first tally stays, without the count it could not read
        assertEquals(List.of(
                new Tally(List.of(new PositiveCount(1))),
                new Tally(List.of(new PositiveCount(2)))
        ), decoded);
    }

    private static JsonObject tally(JsonArray counts) {
        JsonObject tally = new JsonObject();
        tally.add("counts", counts);
        return tally;
    }

    private static List<PositiveCount> decode(JsonElement payload) {
        return CODEC.parse(JsonOps.INSTANCE, payload)
                .getOrThrow(message -> new AssertionError("Expected a lenient decode to succeed: " + message));
    }

}
