package dev.breezes.settlements.infrastructure.rendering.bubbles.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.infrastructure.rendering.bubbles.RenderParameter;
import lombok.Builder;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.List;

@Builder
public class BubbleHorizontalCompositeElement implements BubbleInnerElement {

    private final List<BubbleInnerElement> children;

    @Builder.Default
    private final int gap = 4;

    @Override
    public void render(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack, @Nonnull Matrix4f bubblePose) {
        BubbleBoundingBox compositeBox = this.getBoundingBox();
        float cursorX = -compositeBox.getWidth() / 2.0F;

        for (BubbleInnerElement child : this.children) {
            BubbleBoundingBox childBox = child.getBoundingBox();
            float childCenterX = cursorX + childBox.getWidth() / 2.0F;
            float childCenterY = 0; // each child renders around its own center

            poseStack.pushPose();
            // Each child renders around its local origin, so the composite only needs to place the
            // child center. This keeps the per-element layout independent from bubble-specific ordering.
            poseStack.translate(childCenterX, childCenterY, 0.0F);
            child.render(parameter, poseStack, poseStack.last().pose());
            poseStack.popPose();

            cursorX += childBox.getWidth() + this.gap;
        }
    }

    @Override
    public BubbleBoundingBox getBoundingBox() {
        if (this.children.isEmpty()) {
            return BubbleBoundingBox.builder()
                    .width(1)
                    .height(1)
                    .build();
        }

        int width = this.children.stream()
                .map(BubbleInnerElement::getBoundingBox)
                .mapToInt(BubbleBoundingBox::getWidth)
                .sum() + this.gap * (this.children.size() - 1);
        int height = this.children.stream()
                .map(BubbleInnerElement::getBoundingBox)
                .mapToInt(BubbleBoundingBox::getHeight)
                .max()
                .orElse(1);

        return BubbleBoundingBox.builder()
                .width(Math.max(1, width))
                .height(Math.max(1, height))
                .build();
    }

    @Override
    public void adjustPoseStack(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack) {
        for (BubbleInnerElement child : this.children) {
            child.adjustPoseStack(parameter, poseStack);
        }
    }

    @Override
    public void preRender(@Nonnull RenderParameter parameter) {
        for (BubbleInnerElement child : this.children) {
            child.preRender(parameter);
        }
    }

    @Override
    public BubbleInnerElement copy() {
        return BubbleHorizontalCompositeElement.builder()
                .children(this.children.stream().map(BubbleInnerElement::copy).toList())
                .gap(this.gap)
                .build();
    }

}
