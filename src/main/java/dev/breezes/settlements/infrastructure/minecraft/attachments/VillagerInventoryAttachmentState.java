package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.inventory.BackpackEntry;

import java.util.List;

public record VillagerInventoryAttachmentState(
        /**
         * Distinguishes missing attachment state from a persisted inventory that is intentionally empty
         */
        boolean initialized,
        List<BackpackEntry> entries) {

    public static VillagerInventoryAttachmentState empty() {
        return new VillagerInventoryAttachmentState(false, List.of());
    }

    public static VillagerInventoryAttachmentState of(List<BackpackEntry> entries) {
        return new VillagerInventoryAttachmentState(true, List.copyOf(entries));
    }

}
