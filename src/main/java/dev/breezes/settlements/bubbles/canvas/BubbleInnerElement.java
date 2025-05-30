package dev.breezes.settlements.bubbles.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.bubbles.RenderParameter;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

public interface BubbleInnerElement {

    void render(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack, @Nonnull Matrix4f bubblePose);

    BubbleBoundingBox getBoundingBox();

    void adjustPoseStack(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack);

    void preRender(@Nonnull RenderParameter parameter);

    BubbleInnerElement copy();

}
