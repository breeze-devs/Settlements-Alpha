package dev.breezes.settlements.sounds;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SoundEventPlayable implements IPlayable {

    private final SoundEvent minecraftSound;
    private final float volume;
    private final float pitch;

    public static SoundEventPlayable of(@Nonnull SoundEvent minecraftSound, float volume, float pitch) {
        return new SoundEventPlayable(minecraftSound, volume, pitch);
    }

    public SoundEventPlayable of(@Nonnull SoundEvent minecraftSound) {
        return new SoundEventPlayable(minecraftSound, 1.0F, 1.0F);
    }

    @Override
    public void playGlobally(@Nonnull Level level, double x, double y, double z, @Nonnull SoundSource soundSource) {
        level.playSound(null, x, y, z, this.minecraftSound, soundSource, this.volume, this.pitch);
    }

}
