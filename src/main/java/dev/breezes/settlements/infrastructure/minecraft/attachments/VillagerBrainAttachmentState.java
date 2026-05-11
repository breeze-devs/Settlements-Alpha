package dev.breezes.settlements.infrastructure.minecraft.attachments;

import java.util.List;
import java.util.UUID;

public record VillagerBrainAttachmentState(List<UUID> ownedWolves) {

    public static VillagerBrainAttachmentState empty() {
        return new VillagerBrainAttachmentState(List.of());
    }

    public static VillagerBrainAttachmentState of(List<UUID> ownedWolves) {
        return new VillagerBrainAttachmentState(List.copyOf(ownedWolves));
    }

}
