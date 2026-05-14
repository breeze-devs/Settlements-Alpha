package dev.breezes.settlements.domain.attachment;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;
import java.util.List;

public interface AttachmentProvider {

    default int renderOrder() {
        return 0;
    }

    List<RenderableAttachment> attachmentsFor(@Nonnull BaseVillager villager, float partialTicks);

}
