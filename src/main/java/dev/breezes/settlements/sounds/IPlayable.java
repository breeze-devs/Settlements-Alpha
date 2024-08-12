package dev.breezes.settlements.sounds;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

public interface IPlayable {

    void playGlobally(@Nonnull Level level, double x, double y, double z, @Nonnull SoundSource soundSource);

    @Deprecated
    default void playPrivately(@Nonnull List<Player> players) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
