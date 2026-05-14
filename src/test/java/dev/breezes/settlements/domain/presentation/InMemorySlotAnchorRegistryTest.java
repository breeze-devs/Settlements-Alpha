package dev.breezes.settlements.domain.presentation;

import dev.breezes.settlements.domain.attachment.AttachmentSlot;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InMemorySlotAnchorRegistryTest {

    @Test
    void get_returnsRegisteredAnchor() {
        // Arrange
        SlotAnchor anchor = anchor(SocketId.CROSSED_ARMS_CENTER);
        InMemorySlotAnchorRegistry registry = InMemorySlotAnchorRegistry.builder()
                .anchorsBySlot(Map.of(EquipmentSlot.MAIN_HAND, anchor))
                .build();

        // Act
        SlotAnchor resolved = registry.get(EquipmentSlot.MAIN_HAND);

        // Assert
        assertSame(anchor, resolved);
    }

    @Test
    void defaults_bindMainHandToCrossedArmsSocket() {
        // Arrange
        InMemorySlotAnchorRegistry registry = InMemorySlotAnchorRegistry.defaults();

        // Act
        SlotAnchor resolved = registry.get(EquipmentSlot.MAIN_HAND);

        // Assert
        assertEquals(SocketId.CROSSED_ARMS_CENTER, resolved.getSocket());
        assertEquals(ItemDisplayContext.GROUND, resolved.getDefaultDisplayContext());
    }

    @Test
    void defaults_bindOffHandToCrossedArmsSocket() {
        // Arrange
        InMemorySlotAnchorRegistry registry = InMemorySlotAnchorRegistry.defaults();

        // Act
        SlotAnchor resolved = registry.get(EquipmentSlot.OFF_HAND);

        // Assert
        assertEquals(SocketId.CROSSED_ARMS_CENTER, resolved.getSocket());
        assertEquals(ItemDisplayContext.GROUND, resolved.getDefaultDisplayContext());
    }

    @Test
    void get_returnsIdentityAnchorForUnknownSlot() {
        // Arrange
        InMemorySlotAnchorRegistry registry = InMemorySlotAnchorRegistry.builder()
                .anchorsBySlot(Map.of())
                .build();

        // Act
        SlotAnchor resolved = registry.get(TestSlot.TEST);

        // Assert
        assertEquals(SocketId.CROSSED_ARMS_CENTER, resolved.getSocket());
        assertEquals(ItemDisplayContext.NONE, resolved.getDefaultDisplayContext());
    }

    private static SlotAnchor anchor(SocketId socket) {
        return SlotAnchor.builder()
                .socket(socket)
                .defaultDisplayContext(ItemDisplayContext.GROUND)
                .build();
    }

    private enum TestSlot implements AttachmentSlot {
        TEST
    }

}
