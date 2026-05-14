package dev.breezes.settlements.domain.presentation;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

@Getter
@Builder
public final class AttachmentDisplayProfile {

    private static final float DEFAULT_SCALE = 1.0F;

    private final Vec3 translation;
    private final Vector3f rotation;
    @Builder.Default
    private final float scale = DEFAULT_SCALE;
    @Nullable
    private final ItemDisplayContext displayContextOverride;

    public static AttachmentDisplayProfile identity() {
        return AttachmentDisplayProfile.builder()
                .translation(Vec3.ZERO)
                .rotation(new Vector3f())
                .scale(DEFAULT_SCALE)
                .build();
    }

}
