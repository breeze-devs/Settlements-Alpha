package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class VillagerBrainAttachment {

    public static void loadInto(@Nonnull BaseVillager villager) {
        VillagerBrainAttachmentState state = villager.getData(AttachmentRegistry.VILLAGER_BRAIN);
        if (!state.ownedWolves().isEmpty()) {
            villager.getBrain().setMemory(MemoryTypeRegistry.OWNED_WOLVES.getModuleType(), state.ownedWolves());
        }
    }

    public static void saveFrom(@Nonnull BaseVillager villager) {
        List<UUID> ownedWolves = villager.getBrain()
                .getMemory(MemoryTypeRegistry.OWNED_WOLVES.getModuleType())
                .orElse(List.of());
        villager.setData(AttachmentRegistry.VILLAGER_BRAIN, VillagerBrainAttachmentState.of(ownedWolves));
    }

}
