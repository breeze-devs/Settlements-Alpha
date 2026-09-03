package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.domain.animation.AnimationFrame;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * A baked model an attachment can render as is, posed from an already-sampled animation frame.
 * <p>
 * Posing and drawing are one call because they cannot be safely separated: implementations pose additively
 * onto a rest pose they are responsible for restoring first, so a caller that posed without drawing, or drew
 * twice against one pose, would accumulate transforms across frames.
 */
public interface AttachmentModel {

    /**
     * Restores the rest pose, applies whatever targets in the frame belong to this model, and draws it.
     *
     * @param frame   the sampled frame; targets absent from it leave their bones at rest
     * @param texture the skin to paint this draw with
     */
    void render(@Nonnull AnimationFrame frame,
                @Nonnull PoseStack poseStack,
                @Nonnull MultiBufferSource buffer,
                int packedLight,
                @Nonnull ResourceLocation texture);

    /**
     * Where this model's own root sits relative to the origin it is drawn at, in blocks.
     *
     * @return the offset from the draw origin to the rig's root, or {@link Vec3#ZERO} for a model whose
     * root already sits on it
     */
    default Vec3 rootPivotOffset() {
        return Vec3.ZERO;
    }

}
