package dev.breezes.settlements.application.ai.inference.persona;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneSignal;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerOriginAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerPersonaLineageAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

/**
 * Converts live villager state into a {@link PersonaVillagerRequest} for SIS.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PersonaRequestAssembler {

    public PersonaVillagerRequest assemble(@Nonnull BaseVillager villager) {
        PersonaLineage lineage = PersonaLineageMapper.toWire(VillagerPersonaLineageAttachment.read(villager));
        return PersonaVillagerRequest.builder()
                .villagerId(villager.getUUID())
                .spawnType(VillagerOriginAttachment.read(villager))
                .isNitwit(VillagerProfessionKey.NITWIT.equals(villager.getProfession()))
                .geneSignals(GeneSignal.allFrom(villager.getGenetics()))
                .lineage(lineage)
                .build();
    }

}
