package dev.breezes.settlements.infrastructure.audio;

import dev.breezes.settlements.domain.world.location.Location;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SoundEventPlayable implements IPlayable {

    // Stored as a supplier so deferred-registered SoundEvents can be resolved at play time
    // rather than at enum/class-initialization time, safely clearing the registry lifecycle race.
    private final Supplier<SoundEvent> soundSupplier;
    private final float volume;
    private final float pitch;

    public static SoundEventPlayable of(@Nonnull SoundEvent minecraftSound, float volume, float pitch) {
        return new SoundEventPlayable(() -> minecraftSound, volume, pitch);
    }

    public static SoundEventPlayable of(@Nonnull SoundEvent minecraftSound) {
        return new SoundEventPlayable(() -> minecraftSound, 1.0F, 1.0F);
    }

    /**
     * Variant for deferred-registered SoundEvents whose registry value is not available until
     * after the NeoForge registration phase — resolves the supplier on first play rather than
     * at SoundRegistry enum-initialization time.
     */
    public static SoundEventPlayable ofLazy(@Nonnull Supplier<SoundEvent> soundSupplier, float volume, float pitch) {
        return new SoundEventPlayable(soundSupplier, volume, pitch);
    }

    @Override
    public void playGlobally(@Nonnull Location location, @Nonnull SoundSource soundSource) {
        location.playSound(this.soundSupplier.get(), this.volume, this.pitch, soundSource);
    }

}
