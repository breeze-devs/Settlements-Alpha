package dev.breezes.settlements.shared.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Packs and unpacks 0xAARRGGBB colors from float channels in [0,1].
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArgbColorUtil {

    private static final int CHANNEL_MAX = 255;

    public static int pack(float red, float green, float blue, float alpha) {
        return (channel(alpha) << 24) | (channel(red) << 16) | (channel(green) << 8) | channel(blue);
    }

    public static int alpha(int packed) {
        return (packed >>> 24) & CHANNEL_MAX;
    }

    public static int red(int packed) {
        return (packed >>> 16) & CHANNEL_MAX;
    }

    public static int green(int packed) {
        return (packed >>> 8) & CHANNEL_MAX;
    }

    public static int blue(int packed) {
        return packed & CHANNEL_MAX;
    }

    private static int channel(float value) {
        return Math.round(Math.clamp(value, 0.0F, 1.0F) * CHANNEL_MAX);
    }

}
