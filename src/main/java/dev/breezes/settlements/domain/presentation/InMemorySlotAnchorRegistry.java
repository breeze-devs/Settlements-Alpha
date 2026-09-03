package dev.breezes.settlements.domain.presentation;

import dev.breezes.settlements.domain.attachment.AttachmentSlot;
import dev.breezes.settlements.domain.attachment.UmbrellaSlot;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import lombok.Builder;
import net.minecraft.world.item.ItemDisplayContext;

import javax.annotation.Nonnull;
import java.util.Map;

public final class InMemorySlotAnchorRegistry implements SlotAnchorRegistry {

    private static final SlotAnchor IDENTITY = SlotAnchor.builder()
            .socket(SocketId.CROSSED_ARMS_CENTER)
            .defaultDisplayContext(ItemDisplayContext.NONE)
            .build();

    private final Map<AttachmentSlot, SlotAnchor> anchorsBySlot;

    @Builder
    private InMemorySlotAnchorRegistry(@Nonnull Map<AttachmentSlot, SlotAnchor> anchorsBySlot) {
        this.anchorsBySlot = Map.copyOf(anchorsBySlot);
    }

    public static InMemorySlotAnchorRegistry defaults() {
        SlotAnchor mainHand = SlotAnchor.builder()
                .socket(SocketId.CROSSED_ARMS_CENTER)
                .straightSocket(SocketId.HAND_RIGHT)
                .defaultDisplayContext(ItemDisplayContext.GROUND)
                .build();

        SlotAnchor offHand = SlotAnchor.builder()
                .socket(SocketId.CROSSED_ARMS_CENTER)
                .straightSocket(SocketId.HAND_LEFT)
                .defaultDisplayContext(ItemDisplayContext.GROUND)
                .build();

        // The umbrella has no straight-arm carry variant, so its anchors declare no straightSocket
        SlotAnchor umbrellaLeft = SlotAnchor.builder()
                .socket(SocketId.UMBRELLA_LEFT)
                .defaultDisplayContext(ItemDisplayContext.NONE)
                .build();
        SlotAnchor umbrellaRight = SlotAnchor.builder()
                .socket(SocketId.UMBRELLA_RIGHT)
                .defaultDisplayContext(ItemDisplayContext.NONE)
                .build();

        return InMemorySlotAnchorRegistry.builder()
                .anchorsBySlot(Map.of(
                        EquipmentSlot.MAIN_HAND, mainHand,
                        EquipmentSlot.OFF_HAND, offHand,
                        UmbrellaSlot.UMBRELLA_LEFT, umbrellaLeft,
                        UmbrellaSlot.UMBRELLA_RIGHT, umbrellaRight
                ))
                .build();
    }

    @Override
    public SlotAnchor get(@Nonnull AttachmentSlot slot) {
        return this.anchorsBySlot.getOrDefault(slot, IDENTITY);
    }

}
