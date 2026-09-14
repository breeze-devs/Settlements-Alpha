package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ballista.BallistaStateMachine;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nonnull;

/**
 * The ballista's sound vocabulary.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BallistaSoundPalette {

    static void playBeat(@Nonnull BallistaStateMachine.Beat beat, @Nonnull Location location) {
        SoundRegistry sound = switch (beat) {
            case STROKE_BEGINS -> SoundRegistry.BALLISTA_WIND_STROKE;
            case STROKE_CATCHES -> SoundRegistry.BALLISTA_WIND_CATCH;
            case LOCKS -> SoundRegistry.BALLISTA_COCKED;
        };
        sound.playGlobally(location, SoundSource.BLOCKS);
    }

    static void loadBolt(@Nonnull Location location) {
        SoundRegistry.ITEM_POP_IN.playGlobally(location, SoundSource.BLOCKS);
    }

    static void unloadBolt(@Nonnull Location location) {
        SoundRegistry.ITEM_POP_OUT.playGlobally(location, SoundSource.BLOCKS);
    }

    static void fire(@Nonnull Location location) {
        SoundRegistry.BALLISTA_RELEASE.playGlobally(location, SoundSource.BLOCKS);
    }

    static void refuseInteraction(@Nonnull Location location) {
        SoundRegistry.INTERACTION_FAIL.playGlobally(location, SoundSource.BLOCKS);
    }

    @ClientSide
    static void finishedTurning(@Nonnull Location location) {
        SoundRegistry.BALLISTA_TURN_ARRIVES.playLocally(location, SoundSource.BLOCKS);
    }

}
