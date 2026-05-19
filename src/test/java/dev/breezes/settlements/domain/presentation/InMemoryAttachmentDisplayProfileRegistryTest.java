package dev.breezes.settlements.domain.presentation;

import dev.breezes.settlements.domain.attachment.AttachmentSlot;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InMemoryAttachmentDisplayProfileRegistryTest {

    @Test
    void get_returnsExactProfileWhenPresent() {
        // Arrange
        AttachmentDisplayProfile exact = profile(1.0F);
        AttachmentDisplayProfile generic = profile(2.0F);
        InMemoryAttachmentDisplayProfileRegistry registry = InMemoryAttachmentDisplayProfileRegistry.builder()
                .profilesByKey(Map.of(
                        AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, ItemCategory.AXE), exact,
                        AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, ItemCategory.GENERIC), generic))
                .build();

        // Act
        AttachmentDisplayProfile resolved = registry.get(EquipmentSlot.MAIN_HAND, ItemCategory.AXE);

        // Assert
        assertSame(exact, resolved);
    }

    @Test
    void get_fallsBackToSlotGenericProfileWhenExactMissing() {
        // Arrange
        AttachmentDisplayProfile generic = profile(2.0F);
        InMemoryAttachmentDisplayProfileRegistry registry = InMemoryAttachmentDisplayProfileRegistry.builder()
                .profilesByKey(Map.of(AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, ItemCategory.GENERIC), generic))
                .build();

        // Act
        AttachmentDisplayProfile resolved = registry.get(EquipmentSlot.MAIN_HAND, ItemCategory.SWORD);

        // Assert
        assertSame(generic, resolved);
    }

    @Test
    void get_fallsBackToGenericWhenNoSlotProfileExists() {
        // Arrange
        InMemoryAttachmentDisplayProfileRegistry registry = InMemoryAttachmentDisplayProfileRegistry.builder()
                .profilesByKey(Map.of())
                .build();

        // Act
        AttachmentDisplayProfile resolved = registry.get(TestSlot.TEST, ItemCategory.AXE);

        // Assert
        assertEquals(new Vec3(0, -0.1, 0), resolved.getTranslation());
        assertEquals(new Vector3f(), resolved.getRotation());
        assertEquals(1.0F, resolved.getScale(), 0.0001F);
    }

    private static AttachmentDisplayProfile profile(float scale) {
        return AttachmentDisplayProfile.builder()
                .translation(Vec3.ZERO)
                .rotation(new Vector3f())
                .scale(scale)
                .build();
    }

    private enum TestSlot implements AttachmentSlot {
        TEST
    }

}
