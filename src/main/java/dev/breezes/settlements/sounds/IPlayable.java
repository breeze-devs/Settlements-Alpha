package dev.breezes.settlements.sounds;

import dev.breezes.settlements.models.location.Location;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import java.util.List;

public interface IPlayable {

    void playGlobally(@Nonnull Location location, @Nonnull SoundSource soundSource);

    @Deprecated
    default void playPrivately(@Nonnull List<Player> players) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
