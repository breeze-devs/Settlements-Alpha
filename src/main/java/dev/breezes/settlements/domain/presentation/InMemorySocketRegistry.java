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
        // The socket empty is positioned in Blockbench, so no manual offset is needed here.
        // The item orientation is preserved from the old ARMS-relative tuning; nudge in Blockbench
        // or adjust localRotation once placement is confirmed in-game.
        Socket armsCenterHold = Socket.builder()
                .id(SocketId.CROSSED_ARMS_CENTER)
                .bone(ModelPartRef.ARMS_CROSSED_SOCKET)
                .localTranslation(Vec3.ZERO)
                .localRotation(new Vector3f((float) Math.PI - VANILLA_ARMS_PITCH_RAD, (float) Math.PI, 0.0F))
                .build();

        // Straight-arm hand sockets — rotation is a starting placeholder matching the crossed-arms
        // orientation so the item is not wildly off on first launch. Tuning happens in-game/Blockbench.
        Socket handRight = Socket.builder()
                .id(SocketId.HAND_RIGHT)
                .bone(ModelPartRef.ARM_STRAIGHT_RIGHT_SOCKET)
                .localTranslation(Vec3.ZERO)
                .localRotation(new Vector3f((float) Math.PI - VANILLA_ARMS_PITCH_RAD, (float) Math.PI, 0.0F))
                .build();

        Socket handLeft = Socket.builder()
                .id(SocketId.HAND_LEFT)
                .bone(ModelPartRef.ARM_STRAIGHT_LEFT_SOCKET)
                .localTranslation(Vec3.ZERO)
                .localRotation(new Vector3f((float) Math.PI - VANILLA_ARMS_PITCH_RAD, (float) Math.PI, 0.0F))
                .build();

        // Feet socket is available for future ground-contact providers (e.g. particle trails,
        // footstep anchors, sword-riding). No AttachmentSlot is bound here; providers
        // discover it by SocketId.FEET_CENTER when they need it.
        Socket feetCenter = Socket.builder()
                .id(SocketId.FEET_CENTER)
                .bone(ModelPartRef.FEET_CENTER_SOCKET)
                .localTranslation(Vec3.ZERO)
                .localRotation(new Vector3f())
                .build();

        return InMemorySocketRegistry.builder()
                .socketsById(Map.of(
                        SocketId.CROSSED_ARMS_CENTER, armsCenterHold,
                        SocketId.HAND_RIGHT, handRight,
                        SocketId.HAND_LEFT, handLeft,
                        SocketId.FEET_CENTER, feetCenter
                ))
                .build();
    }

    @Override
    public Socket get(@Nonnull SocketId id) {
        return this.socketsById.getOrDefault(id, Socket.identity(id));
    }

}
