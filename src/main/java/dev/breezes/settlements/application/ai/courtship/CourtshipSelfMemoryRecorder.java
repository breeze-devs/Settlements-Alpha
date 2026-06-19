package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.UUID;

/**
 * Records a private, first-hand courtship failure directly into the acting villager's
 * {@link dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore}, bypassing
 * the {@link dev.breezes.settlements.domain.ai.worldevent.WorldEventBus}.
 * <p>
 * Private failures — those where no bystander could have witnessed the outcome — must not
 * travel on the bus, as the bus is observable by nearby villagers. The villager itself should
 * still retain the memory so the monologue projector can surface it as a personal failure seed.
 * <p>
 * This class is a thin Minecraft-aware shell. All entry construction is delegated to
 * {@link CourtshipSelfMemoryEntryBuilder}, which is Minecraft-free and unit-tested independently.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CourtshipSelfMemoryRecorder {

    /**
     * Records a private courtship failure for the acting villager.
     * <p>
     * The entry uses a random origin id so it never corroborates with any bus-emitted event
     * (those carry ids derived from the world-event sequence and actor UUID). Each failed
     * attempt is recorded as an independent fact.
     *
     * @param self      the villager who initiated the courtship
     * @param partnerId UUID of the intended courtship target, or null if the target was unknown
     * @param reason    human-readable reason the attempt failed (e.g. "no one answered")
     */
    public void recordPrivateFailure(@Nonnull BaseVillager self,
                                     @Nullable UUID partnerId,
                                     @Nonnull String reason) {
        long currentTick = self.level().getGameTime();
        UUID originObservationId = UUID.randomUUID();

        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(
                self.getUUID(),
                partnerId,
                currentTick,
                reason,
                originObservationId);

        self.getKnowledgeStore().admit(entry);
    }

}
