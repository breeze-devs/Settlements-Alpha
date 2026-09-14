package dev.breezes.settlements.infrastructure.audio;

import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import java.util.List;

public interface IPlayable {

    void playGlobally(@Nonnull Location location, @Nonnull SoundSource soundSource);

    /**
     * Plays for this client alone and sends nothing, so every client that should hear it plays it itself. Does nothing
     * on a server.
     */
    void playLocally(@Nonnull Location location, @Nonnull SoundSource soundSource);

    @Deprecated
    default void playPrivately(@Nonnull List<Player> players) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
