package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import java.util.List;

public final class VillagerGeneticsAttachment {

    public static boolean loadInto(@Nonnull BaseVillager villager, @Nonnull GeneticsProfile genetics) {
        return loadInto((LivingEntity) villager, genetics);
    }

    /**
     * Reads the genetics attachment from any LivingEntity (e.g. a ZombieVillager carrying preserved genetics)
     * into the given profile. Returns false if no genetics data has been stored on this entity yet.
     */
    public static boolean loadInto(@Nonnull LivingEntity entity, @Nonnull GeneticsProfile genetics) {
        VillagerGeneticsAttachmentState state = entity.getData(AttachmentRegistry.VILLAGER_GENETICS);
        if (!state.initialized()) {
            return false;
        }

        state.genes().forEach(geneState -> genetics.setGene(geneState.type(), new Gene(geneState.value())));
        return true;
    }

    public static void saveFrom(@Nonnull BaseVillager villager, @Nonnull GeneticsProfile genetics) {
        saveFrom((LivingEntity) villager, genetics);
    }

    /**
     * Persists the given genetics profile onto any LivingEntity (e.g. a ZombieVillager acting as a
     * genetics carrier during the zombie phase).
     */
    public static void saveFrom(@Nonnull LivingEntity entity, @Nonnull GeneticsProfile genetics) {
        List<VillagerGeneState> genes = genetics.getAllGenes().entrySet().stream()
                .map(entry -> new VillagerGeneState(entry.getKey(), entry.getValue().value()))
                .toList();
        entity.setData(AttachmentRegistry.VILLAGER_GENETICS, VillagerGeneticsAttachmentState.of(genes));
    }

}
