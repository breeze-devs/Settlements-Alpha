package dev.breezes.settlements.bootstrap.registry.sounds;

import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.audio.IPlayable;
import dev.breezes.settlements.infrastructure.audio.SoundEventPlayable;
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
    STONE_CUTTER_WORKING(SoundEventPlayable.of(SoundEvents.VILLAGER_WORK_MASON, 0.3f, 2f)),
    ITEM_POP_IN(SoundEventPlayable.of(SoundEvents.ITEM_PICKUP, 0.3f, 0.8f)),
    ITEM_POP_OUT(SoundEventPlayable.of(SoundEvents.ITEM_PICKUP, 0.3f, 1.2f)),
    SHEAR_SHEEP(SoundEventPlayable.of(SoundEvents.SHEEP_SHEAR, 1.0f, 1.0f)),
    COLLECT_HONEY(SoundEventPlayable.of(SoundEvents.BOTTLE_FILL, 1.0f, 1.0f)),
    HARVEST_HONEYCOMB(SoundEventPlayable.of(SoundEvents.BEEHIVE_SHEAR, 0.8f, 1.2f)),
    MILK_COW(SoundEventPlayable.of(SoundEvents.COW_MILK, 1.0f, 1.0f)),
    OPEN_FENCE_GATE(SoundEventPlayable.of(SoundEvents.FENCE_GATE_OPEN, 1.0f, 1.0f)),
    CLOSE_FENCE_GATE(SoundEventPlayable.of(SoundEvents.FENCE_GATE_CLOSE, 1.0f, 1.0f)),
    FISHING_CAST(SoundEventPlayable.of(SoundEvents.FISHING_BOBBER_THROW, 0.5f, 0.4f)),
    FISHING_SPLASH(SoundEventPlayable.of(SoundEvents.FISHING_BOBBER_SPLASH, 0.25f, 1.0f)),
    FISHING_REEL(SoundEventPlayable.of(SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f)),
    MAP_FLAP(SoundEventPlayable.of(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.8f)),
    MAP_SCRIBBLE(SoundEventPlayable.of(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.3f)),
    TAKE_FROM_CHEST(SoundEventPlayable.of(SoundEvents.CHEST_OPEN, 0.8f, 1.0f)),
    THROW_EGG(SoundEventPlayable.of(SoundEvents.EGG_THROW, 1.0f, 1.2f)),
    BLAST_MISFIRE(SoundEventPlayable.of(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.4f)),
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
