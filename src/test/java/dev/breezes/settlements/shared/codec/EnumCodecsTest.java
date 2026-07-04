package dev.breezes.settlements.shared.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumCodecsTest {

    private enum Sample {
        FIRST,
        SECOND,
    }

    private static final Codec<Sample> CODEC = EnumCodecs.lenient(Sample.class, Sample.FIRST);

    @Test
    void lenient_roundTripsAKnownValue() {
        // Arrange
        JsonElement encoded = encode(Sample.SECOND);

        // Act
        Sample decoded = decode(encoded);

        // Assert
        assertEquals(Sample.SECOND, decoded);
    }

    @Test
    void lenient_encodesUsingTheEnumConstantName() {
        // Arrange, Act
        JsonElement encoded = encode(Sample.SECOND);

        // Assert
        assertEquals("SECOND", encoded.getAsString());
    }

    @Test
    void lenient_decodesAnUnknownNameAsTheFallbackInsteadOfErroring() {
        // Arrange — simulates a renamed/removed enum constant surviving on an old save.
        JsonElement garbage = new JsonPrimitive("REMOVED_CONSTANT");

        // Act
        Sample decoded = decode(garbage);

        // Assert
        assertEquals(Sample.FIRST, decoded);
    }

    private static JsonElement encode(Sample value) {
        return CODEC.encodeStart(JsonOps.INSTANCE, value)
                .resultOrPartial(Assertions::fail)
                .orElseThrow();
    }

    private static Sample decode(JsonElement payload) {
        return CODEC.decode(JsonOps.INSTANCE, payload)
                .resultOrPartial(Assertions::fail)
                .orElseThrow()
                .getFirst();
    }

    private static final class Assertions {

        private Assertions() {
        }

        private static void fail(String message) {
            org.junit.jupiter.api.Assertions.fail(message);
        }

    }

}
