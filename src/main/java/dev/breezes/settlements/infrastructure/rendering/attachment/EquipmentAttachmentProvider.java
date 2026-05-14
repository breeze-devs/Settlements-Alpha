package dev.breezes.settlements.infrastructure.rendering.attachment;

import dev.breezes.settlements.domain.attachment.AttachmentContent;
import dev.breezes.settlements.domain.attachment.AttachmentProvider;
import dev.breezes.settlements.domain.attachment.RenderableAttachment;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import dev.breezes.settlements.domain.inventory.IVillagerEquipment;
import dev.breezes.settlements.domain.presentation.ItemCategory;
import dev.breezes.settlements.infrastructure.minecraft.attachments.EquipmentLookup;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class EquipmentAttachmentProvider implements AttachmentProvider {

    @Override
    public List<RenderableAttachment> attachmentsFor(@Nonnull BaseVillager villager, float partialTicks) {
        return EquipmentLookup.find(villager)
                .map(EquipmentAttachmentProvider::attachmentsFor)
                .orElseGet(List::of);
    }

    static List<RenderableAttachment> attachmentsFor(@Nonnull IVillagerEquipment equipment) {
        List<RenderableAttachment> attachments = new ArrayList<>();
        for (EquipmentSlot slot : equipment.occupiedSlots()) {
            addAttachment(equipment, attachments, slot);
        }

        return List.copyOf(attachments);
    }

    private static void addAttachment(@Nonnull IVillagerEquipment equipment,
                                      @Nonnull List<RenderableAttachment> attachments,
                                      @Nonnull EquipmentSlot slot) {
        equipment.getEquipped(slot).ifPresent(stack -> attachments.add(RenderableAttachment.builder()
                .slot(slot)
                .content(new AttachmentContent.ItemContent(stack))
                .category(ItemCategory.of(stack))
                .build()));
    }

}
