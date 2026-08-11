package dev.breezes.settlements.infrastructure.audio;

import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nonnull;

/**
 * An {@link IPlayable} whose pitch a caller may override, for a caller mapping a magnitude — a
 * resulting size, a position in a sequence — onto pitch without reaching past this interface to a
 * raw platform sound call.
 * <p>
 * Only a playable that implements this interface exposes the override, so a caller that needs it
 * requires this type and finds out at compile time rather than discovering the gap at runtime.
 */
public interface PitchedPlayable extends IPlayable {

    void playGlobally(@Nonnull Location location, @Nonnull SoundSource soundSource, float pitchOverride);

}
