package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;
import java.util.List;

public final class VillagerGeneticsAttachment {

    public static boolean loadInto(@Nonnull BaseVillager villager, @Nonnull GeneticsProfile genetics) {
        VillagerGeneticsAttachmentState state = villager.getData(AttachmentRegistry.VILLAGER_GENETICS);
        if (!state.initialized()) {
            return false;
        }

        state.genes().forEach(geneState -> genetics.setGene(geneState.type(), new Gene(geneState.value())));
        return true;
    }

    public static void saveFrom(@Nonnull BaseVillager villager, @Nonnull GeneticsProfile genetics) {
        List<VillagerGeneState> genes = genetics.getAllGenes().entrySet().stream()
                .map(entry -> new VillagerGeneState(entry.getKey(), entry.getValue().value()))
                .toList();
        villager.setData(AttachmentRegistry.VILLAGER_GENETICS, VillagerGeneticsAttachmentState.of(genes));
    }

}
