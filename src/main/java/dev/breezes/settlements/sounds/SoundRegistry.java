package dev.breezes.settlements.sounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

@AllArgsConstructor
@Getter
public enum SoundRegistry {

    // TODO: we might want to account for chained sounds -- i.e. multiple sounds playing in sequence, such as notes

    REPAIR_IRON_GOLEM(SoundEventPlayable.of(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1F));

    // TODO: this might not be just a vanilla sound, but a custom/modded sound
    @Nonnull
    private final IPlayable playable;

    public void playGlobally(@Nonnull Level level, double x, double y, double z, @Nonnull SoundSource soundSource) {
        this.playable.playGlobally(level, x, y, z, soundSource);
    }

    public void playPrivately() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
