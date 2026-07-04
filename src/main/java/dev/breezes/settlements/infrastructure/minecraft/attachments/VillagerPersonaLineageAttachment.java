package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.personality.ParentPersona;
import dev.breezes.settlements.domain.personality.PersonaLineageSnapshot;
import dev.breezes.settlements.domain.personality.PersonalityStatus;
import dev.breezes.settlements.domain.personality.VillagerPersonality;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Static facade over the transient {@link AttachmentRegistry#VILLAGER_PERSONA_LINEAGE} attachment.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerPersonaLineageAttachment {

    public static PersonaLineageSnapshot read(LivingEntity entity) {
        return entity.getData(AttachmentRegistry.VILLAGER_PERSONA_LINEAGE);
    }

    /**
     * Captures both parents' persona at breed time, while they are still alive and readable --
     * later they may unload or die. Writes the snapshot onto the child only when it carries actual
     * signal, so a pair of not-yet-generated parents simply leaves the child at the default-empty
     * attachment instead of storing a signal-less snapshot.
     */
    public static void capture(@Nonnull BaseVillager child, @Nonnull BaseVillager parentA, @Nullable BaseVillager parentB) {
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(toParentPersona(parentA), toParentPersona(parentB));
        if (snapshot.hasAnySignal()) {
            child.setData(AttachmentRegistry.VILLAGER_PERSONA_LINEAGE, snapshot);
        }
    }

    private static ParentPersona toParentPersona(@Nullable BaseVillager parent) {
        if (parent == null) {
            return ParentPersona.unknown();
        }

        VillagerPersonality personality = VillagerPersonalityAttachment.read(parent);
        if (personality.status() != PersonalityStatus.READY) {
            return ParentPersona.unknown();
        }

        return new ParentPersona(personality.adjectives(), personality.characterSketch());
    }

}
