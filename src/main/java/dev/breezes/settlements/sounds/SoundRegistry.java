package dev.breezes.settlements.sounds;

import dev.breezes.settlements.models.location.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nonnull;

@AllArgsConstructor
@Getter
public enum SoundRegistry {

    // TODO: we might want to account for chained sounds -- i.e. multiple sounds playing in sequence, such as notes

    REPAIR_IRON_GOLEM(SoundEventPlayable.of(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1F)),
    FEED_ANIMAL(SoundEventPlayable.of(SoundEvents.GENERIC_EAT, 1.0F, 1F)),
    THROW_POTION(SoundEventPlayable.of(SoundEvents.SPLASH_POTION_THROW, 1.0f, 1f)),
    ;

    // TODO: this might not be just a vanilla sound, but a custom/modded sound
    @Nonnull
    private final IPlayable playable;

    public void playGlobally(@Nonnull Location location, @Nonnull SoundSource soundSource) {
        this.playable.playGlobally(location, soundSource);
    }

    public void playPrivately() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
