package dev.breezes.settlements.infrastructure.rendering.bubbles.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.infrastructure.rendering.bubbles.RenderParameter;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.List;

@Builder
public class BubbleTextElement implements BubbleInnerElement {

    private final Component component;

    @Builder.Default
    private final int maxWidth = 200;
    @Builder.Default
    private final float scale = 1.0F;

    // Layout reads the lines several times per frame, so they are split once and kept
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final List<FormattedCharSequence> lines = Minecraft.getInstance().font.split(this.component, this.maxWidth);

    @Override
    public void preRender(@Nonnull RenderParameter parameter) {
    }

    @Override
    public void render(@Nonnull RenderParameter parameter, @Nonnull PoseStack bubblePoseStack, @Nonnull Matrix4f bubblePose) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> messageLines = this.getLines();

        BubbleBoundingBox boundingBox = this.getBoundingBox();
        float xStart = -boundingBox.getWidth() / 2.0F;
        float yStart = -boundingBox.getHeight() / 2.0F;

        bubblePoseStack.pushPose();
        // Scaling at the element boundary keeps larger marker glyphs and smaller count text as a
        // presentation concern instead of multiplying bubble variants.
        bubblePoseStack.scale(this.scale, this.scale, 1.0F);
        Matrix4f resolvedPose = bubblePoseStack.last().pose();

        for (int i = 0; i < messageLines.size(); i++) {
            FormattedCharSequence sequence = messageLines.get(i);
            if (sequence != null) {
                font.drawInBatch(sequence,
                        xStart / this.scale,
                        (yStart / this.scale) + font.lineHeight * i,
                        -1, // Color formatting is handled by the Component style natively
                        false,
                        resolvedPose,
                        parameter.getBuffer(),
                        Font.DisplayMode.NORMAL,
                        0,
                        parameter.getPackedLight());
            }
        }
        bubblePoseStack.popPose();
    }

    @Override
    public BubbleBoundingBox getBoundingBox() {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> messageLines = this.getLines();

        int textWidth = messageLines.stream()
                .map(font::width)
                .max(Integer::compare)
                .orElse(this.maxWidth);
        return BubbleBoundingBox.builder()
                .width(Math.max(1, Math.round(textWidth * this.scale)))
                .height(Math.max(1, Math.round(font.lineHeight * messageLines.size() * this.scale)))
                .build();
    }

    @Override
    public void adjustPoseStack(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack) {
        poseStack.translate(0.0D, (0.1F * this.getLines().size() * this.scale), 0.0D);
    }

    @Override
    public BubbleInnerElement copy() {
        return BubbleTextElement.builder()
                .component(this.component.copy())
                .maxWidth(this.maxWidth)
                .scale(this.scale)
                .build();
    }

}
