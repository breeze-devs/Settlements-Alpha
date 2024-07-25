package dev.breezes.settlements.sounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

@AllArgsConstructor
@Getter
public enum SoundRegistry {

    // TODO: we might want to account for chained sounds -- i.e. multiple sounds playing in sequence, such as notes

    PLACEHOLDER(SoundEvents.GENERIC_EXPLODE);

    // TODO: this might not be just a vanilla sound, but a custom/modded sound
    private final SoundEvent minecraftSound;

}
