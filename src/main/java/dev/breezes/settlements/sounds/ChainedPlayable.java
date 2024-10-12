package dev.breezes.settlements.sounds;

import dev.breezes.settlements.models.location.Location;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nonnull;

/**
 * Represents a series of sounds that will be played sequentially, with a specific delay between each sound
 * - e.g. List of entries of (IPlayable, delay_after), which gets played sequentially
 */
public class ChainedPlayable implements IPlayable {

    @Override
    public void playGlobally(@Nonnull Location location, @Nonnull SoundSource soundSource) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
