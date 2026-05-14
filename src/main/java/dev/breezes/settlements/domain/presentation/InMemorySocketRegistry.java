package dev.breezes.settlements.domain.presentation;

import lombok.Builder;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Map;

public final class InMemorySocketRegistry implements SocketRegistry {

    private static final float VANILLA_ARMS_PITCH_RAD = (float) Math.toRadians(-40);

    private final Map<SocketId, Socket> socketsById;

    @Builder
    private InMemorySocketRegistry(@Nonnull Map<SocketId, Socket> socketsById) {
        this.socketsById = Map.copyOf(socketsById);
    }

    /**
     * Registers the built-in rig sockets. Future slots should bind to these sockets through SlotAnchorRegistry.
     */
    public static InMemorySocketRegistry defaults() {
        // These values re-express the previous root-space hand anchor in the vanilla crossed-arms local frame.
        // Keeping the conversion here makes future visual calibration a data change instead of a renderer change.
        Socket armsCenterHold = Socket.builder()
                .id(SocketId.CROSSED_ARMS_CENTER)
                .bone(ModelPartRef.ARMS)
                .localTranslation(new Vec3(0.0D, 0.2D, -0.2D))
                .localRotation(new Vector3f((float) Math.PI - VANILLA_ARMS_PITCH_RAD, (float) Math.PI, 0.0F))
                .build();

        return InMemorySocketRegistry.builder()
                .socketsById(Map.of(SocketId.CROSSED_ARMS_CENTER, armsCenterHold))
                .build();
    }

    @Override
    public Socket get(@Nonnull SocketId id) {
        return this.socketsById.getOrDefault(id, Socket.identity(id));
    }

}
