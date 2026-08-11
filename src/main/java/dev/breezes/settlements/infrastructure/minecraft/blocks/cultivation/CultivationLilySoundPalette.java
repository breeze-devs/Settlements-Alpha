package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.farming.CultivationZone;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nonnull;

/**
 * The Cultivation Lily's sound vocabulary.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CultivationLilySoundPalette {

    /**
     * One pitch per reachable zone size, indexed by half-extent.
     */
    private static final float[] RESIZE_PITCHES_BY_HALF_EXTENT = {
            0.530F,
            0.667F,
            0.794F,
            1.059F,
    };

    static void resize(@Nonnull Location location, int resultingHalfExtent) {
        float pitch = resizePitch(resultingHalfExtent);
        SoundRegistry.CULTIVATION_LILY_RESIZE.pitchedPlayable()
                .orElseThrow(() -> new IllegalStateException("CULTIVATION_LILY_RESIZE must stay bound to a pitch-overridable sound"))
                .playGlobally(location, SoundSource.BLOCKS, pitch);
    }

    static void filterSet(@Nonnull Location location) {
        SoundRegistry.CULTIVATION_LILY_FILTER_SET.playGlobally(location, SoundSource.BLOCKS);
    }

    static void filterCleared(@Nonnull Location location) {
        SoundRegistry.CULTIVATION_LILY_FILTER_CLEARED.playGlobally(location, SoundSource.BLOCKS);
    }

    static void filterAlreadyCleared(@Nonnull Location location) {
        SoundRegistry.CULTIVATION_LILY_FILTER_ALREADY_CLEARED.playGlobally(location, SoundSource.BLOCKS);
    }

    /**
     * The pitch a zone resizing to the given half-extent sounds at.
     * <p>
     * Clamped to the table rather than indexed straight into it, so a half-extent range widened without
     * a matching pitch added repeats the top note instead of throwing mid-interaction. That degradation
     * is silent by design, which is why a test asserts every reachable size still sounds distinct.
     */
    @VisibleForTesting
    static float resizePitch(int resultingHalfExtent) {
        int index = Math.clamp(resultingHalfExtent - CultivationZone.MIN_HALF_EXTENT, 0, RESIZE_PITCHES_BY_HALF_EXTENT.length - 1);
        return RESIZE_PITCHES_BY_HALF_EXTENT[index];
    }

}
