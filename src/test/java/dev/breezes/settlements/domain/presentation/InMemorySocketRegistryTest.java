package dev.breezes.settlements.domain.presentation;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySocketRegistryTest {

    @Test
    void get_returnsRegisteredSocket() {
        // Arrange
        Socket socket = socket(ModelPartRef.ARMS);
        InMemorySocketRegistry registry = InMemorySocketRegistry.builder()
                .socketsById(Map.of(SocketId.CROSSED_ARMS_CENTER, socket))
                .build();

        // Act
        Socket resolved = registry.get(SocketId.CROSSED_ARMS_CENTER);

        // Assert
        assertSame(socket, resolved);
    }

    @Test
    void get_returnsIdentitySocketWhenKnownIdIsMissing() {
        // Arrange
        InMemorySocketRegistry registry = InMemorySocketRegistry.builder()
                .socketsById(Map.of())
                .build();

        // Act
        Socket resolved = registry.get(SocketId.CROSSED_ARMS_CENTER);

        // Assert
        assertEquals(ModelPartRef.ROOT, resolved.getBone());
        assertEquals(Vec3.ZERO, resolved.getLocalTranslation());
        assertEquals(new Vector3f(), resolved.getLocalRotation());
        assertEquals(1.0F, resolved.getLocalScale(), 0.0001F);
        assertFalse(resolved.isInheritsBoneTransform());
    }

    @Test
    void defaults_bindArmsCenterHoldToArmsBoneWithInheritance() {
        // Arrange
        InMemorySocketRegistry registry = InMemorySocketRegistry.defaults();

        // Act
        Socket resolved = registry.get(SocketId.CROSSED_ARMS_CENTER);

        // Assert
        assertEquals(SocketId.CROSSED_ARMS_CENTER, resolved.getId());
        assertEquals(ModelPartRef.ARMS_CROSSED_SOCKET, resolved.getBone());
        assertTrue(resolved.isInheritsBoneTransform());
    }

    private static Socket socket(ModelPartRef bone) {
        return Socket.builder()
                .id(SocketId.CROSSED_ARMS_CENTER)
                .bone(bone)
                .localTranslation(Vec3.ZERO)
                .localRotation(new Vector3f())
                .build();
    }

}
