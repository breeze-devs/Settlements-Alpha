package dev.breezes.settlements.infrastructure.rendering.bubbles.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.infrastructure.rendering.bubbles.RenderParameter;
import lombok.Builder;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.List;

@Builder
@CustomLog
public class BubbleTextElement implements BubbleInnerElement {

    private Font font;
    private Component component;

    @Builder.Default
    private int maxWidth = 200;
    @Builder.Default
    private float opacity = 0.8F;
    @Builder.Default
    private float scale = 1.0F;


    @Override
    public void preRender(@Nonnull RenderParameter parameter) {
    }

    @Override
    public void render(@Nonnull RenderParameter parameter, @Nonnull PoseStack bubblePoseStack, @Nonnull Matrix4f bubblePose) {
        Font resolvedFont = this.resolveFont();
        List<FormattedCharSequence> messageLines = resolvedFont.split(this.component, this.maxWidth);

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
                resolvedFont.drawInBatch(sequence,
                        xStart / this.scale,
                        (yStart / this.scale) + resolvedFont.lineHeight * i,
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
        Font resolvedFont = this.resolveFont();
        List<FormattedCharSequence> messageLines = resolvedFont.split(this.component, this.maxWidth);

        int textWidth = messageLines.stream()
                .map(resolvedFont::width)
                .max(Integer::compare)
                .orElse(this.maxWidth);
        return BubbleBoundingBox.builder()
                .width(Math.max(1, Math.round(textWidth * this.scale)))
                .height(Math.max(1, Math.round(resolvedFont.lineHeight * messageLines.size() * this.scale)))
                .build();
    }

    @Override
    public void adjustPoseStack(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack) {
        Font resolvedFont = this.resolveFont();
        List<FormattedCharSequence> messageLines = resolvedFont.split(this.component, this.maxWidth);
        poseStack.translate(0.0D, (0.1F * messageLines.size() * this.scale), 0.0D);
    }

    @Override
    public BubbleInnerElement copy() {
        return BubbleTextElement.builder()
                .font(this.font)
                .component(this.component != null ? this.component.copy() : null)
                .maxWidth(this.maxWidth)
                .opacity(this.opacity)
                .scale(this.scale)
                .build();
    }

    private Font resolveFont() {
        if (this.font != null) {
            return this.font;
        }
        return Minecraft.getInstance().font;
    }

}
