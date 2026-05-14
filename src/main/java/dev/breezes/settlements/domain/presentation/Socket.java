package dev.breezes.settlements.domain.presentation;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@Getter
@Builder
public final class Socket {

    private static final float DEFAULT_SCALE = 1.0F;

    private final SocketId id;
    private final ModelPartRef bone;
    private final Vec3 localTranslation;
    private final Vector3f localRotation;
    @Builder.Default
    private final float localScale = DEFAULT_SCALE;
    @Builder.Default
    private final boolean inheritsBoneTransform = true;

    public static Socket identity(SocketId id) {
        return Socket.builder()
                .id(id)
                .bone(ModelPartRef.ROOT)
                .localTranslation(Vec3.ZERO)
                .localRotation(new Vector3f())
                .localScale(DEFAULT_SCALE)
                .inheritsBoneTransform(false)
                .build();
    }

}
