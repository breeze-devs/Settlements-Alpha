package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VillagerGeneticsAttachmentSerializer
        extends StateCodecAttachmentSerializer<List<VillagerGeneState>, GeneticsProfile> {

    public VillagerGeneticsAttachmentSerializer() {
        super(VillagerGeneticsAttachmentCodec.STATE_CODEC);
    }

    @Override
    List<VillagerGeneState> toState(@Nonnull GeneticsProfile genetics) {
        List<VillagerGeneState> genes = new ArrayList<>();
        for (Map.Entry<GeneType, Gene> entry : genetics.getAllGenes().entrySet()) {
            genes.add(new VillagerGeneState(entry.getKey(), entry.getValue().value()));
        }
        return genes;
    }

    @Override
    GeneticsProfile fromState(@Nonnull List<VillagerGeneState> genes) {
        // Starts from a fresh roll so a gene the save does not carry is rolled randomly
        GeneticsProfile genetics = new GeneticsProfile();
        for (VillagerGeneState gene : genes) {
            genetics.setGene(gene.type(), new Gene(gene.value()));
        }
        return genetics;
    }

}
