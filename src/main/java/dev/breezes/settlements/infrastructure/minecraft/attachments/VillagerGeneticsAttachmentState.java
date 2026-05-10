package dev.breezes.settlements.infrastructure.minecraft.attachments;

import java.util.List;

public record VillagerGeneticsAttachmentState(boolean initialized, List<VillagerGeneState> genes) {

    public static VillagerGeneticsAttachmentState empty() {
        return new VillagerGeneticsAttachmentState(false, List.of());
    }

    public static VillagerGeneticsAttachmentState of(List<VillagerGeneState> genes) {
        return new VillagerGeneticsAttachmentState(true, List.copyOf(genes));
    }

}
