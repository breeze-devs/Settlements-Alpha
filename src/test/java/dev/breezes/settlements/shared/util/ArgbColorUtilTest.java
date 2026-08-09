package dev.breezes.settlements.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArgbColorUtilTest {

    @Test
    void pack_roundTripsThroughEveryChannelExtractor() {
        // Arrange
        int packed = ArgbColorUtil.pack(0.2F, 0.4F, 0.6F, 0.8F);

        // Act & Assert: a wrong bit-shift or a swapped channel order would make one extractor return another
        // channel's value instead of its own.
        assertEquals(Math.round(0.2F * 255), ArgbColorUtil.red(packed));
        assertEquals(Math.round(0.4F * 255), ArgbColorUtil.green(packed));
        assertEquals(Math.round(0.6F * 255), ArgbColorUtil.blue(packed));
        assertEquals(Math.round(0.8F * 255), ArgbColorUtil.alpha(packed));
    }

    @Test
    void pack_clampsChannelsAboveOne() {
        // Arrange
        int packed = ArgbColorUtil.pack(2.0F, 2.0F, 2.0F, 2.0F);

        // Act & Assert: a missing clamp would let a channel overflow past 255 and corrupt the adjacent
        // channel's bits instead of saturating at white/opaque.
        assertEquals(255, ArgbColorUtil.red(packed));
        assertEquals(255, ArgbColorUtil.green(packed));
        assertEquals(255, ArgbColorUtil.blue(packed));
        assertEquals(255, ArgbColorUtil.alpha(packed));
    }

    @Test
    void pack_clampsChannelsBelowZero() {
        // Arrange
        int packed = ArgbColorUtil.pack(-1.0F, -1.0F, -1.0F, -1.0F);

        // Act & Assert: a missing clamp would let a negative channel produce a negative int, which then
        // corrupts every other channel's bits once shifted into place.
        assertEquals(0, ArgbColorUtil.red(packed));
        assertEquals(0, ArgbColorUtil.green(packed));
        assertEquals(0, ArgbColorUtil.blue(packed));
        assertEquals(0, ArgbColorUtil.alpha(packed));
    }

    @Test
    void alpha_isIndependentOfColorChannels() {
        // Arrange: same RGB, different alpha — the fact packing is meant to express (one hue, alpha carries
        // the variant).
        int dim = ArgbColorUtil.pack(0.5F, 0.5F, 0.5F, 0.25F);
        int bright = ArgbColorUtil.pack(0.5F, 0.5F, 0.5F, 1.0F);

        // Act & Assert: channel bleed between alpha and RGB would make changing only the alpha argument also
        // change what red()/green()/blue() report.
        assertEquals(ArgbColorUtil.red(dim), ArgbColorUtil.red(bright));
        assertEquals(ArgbColorUtil.green(dim), ArgbColorUtil.green(bright));
        assertEquals(ArgbColorUtil.blue(dim), ArgbColorUtil.blue(bright));
    }

}
