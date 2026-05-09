package dev.breezes.settlements.infrastructure.minecraft.attachments;

import java.util.List;

public record VillagerInventoryAttachmentState(
        /**
         * Distinguishes missing attachment state from a persisted inventory that is intentionally empty
         */
        boolean initialized,
        List<VillagerInventorySlotState> slots) {

    public static VillagerInventoryAttachmentState empty() {
        return new VillagerInventoryAttachmentState(false, List.of());
    }

    public static VillagerInventoryAttachmentState of(List<VillagerInventorySlotState> slots) {
        return new VillagerInventoryAttachmentState(true, List.copyOf(slots));
    }

}
