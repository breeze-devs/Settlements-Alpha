package dev.breezes.settlements.bubbles.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.bubbles.RenderParameter;
import lombok.Builder;
import lombok.CustomLog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.List;

@Builder
@CustomLog
public class BubbleTextElement implements BubbleInnerElement {

    // TODO: replace with domain model
    private Font font;
    private ChatFormatting color;
    private String message;

    @Builder.Default
    private int maxWidth = 200;
    @Builder.Default
    private float opacity = 0.8F;


    @Override
    public void preRender(@Nonnull RenderParameter parameter) {
    }

    @Override
    public void render(@Nonnull RenderParameter parameter, @Nonnull PoseStack bubblePoseStack, @Nonnull Matrix4f bubblePose) {
        MutableComponent component = Component.literal(this.message)
                .withStyle(this.color);
        List<FormattedCharSequence> messageLines = this.font.split(component, this.maxWidth);

        BubbleBoundingBox boundingBox = this.getBoundingBox();
        float xStart = -boundingBox.getWidth() / 2.0F;
        float yStart = -boundingBox.getHeight() / 2.0F;
        float yIncrement = this.font.lineHeight;

        for (int i = 0; i < messageLines.size(); i++) {
            FormattedCharSequence sequence = messageLines.get(i);
            if (sequence != null) {
                this.font.drawInBatch(sequence, xStart, yStart + yIncrement * i, -1, false, bubblePose, parameter.getBuffer(), Font.DisplayMode.NORMAL, 0, parameter.getPackedLight());
            }
        }
    }

    @Override
    public BubbleBoundingBox getBoundingBox() {
        MutableComponent component = Component.literal(this.message);
        List<FormattedCharSequence> messageLines = this.font.split(component, this.maxWidth);

        int textWidth = messageLines.stream()
                .map(this.font::width)
                .max(Integer::compare)
                .orElse(this.maxWidth);
        return BubbleBoundingBox.builder()
                .width(textWidth)
                .height(this.font.lineHeight * messageLines.size())
                .build();
    }

    @Override
    public void adjustPoseStack(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack) {
        MutableComponent component = Component.literal(this.message);
        List<FormattedCharSequence> messageLines = this.font.split(component, this.maxWidth);
        poseStack.translate(0.0D, (0.1F * messageLines.size()), 0.0D);
    }

    @Override
    public BubbleInnerElement copy() {
        return BubbleTextElement.builder()
                .font(this.font)
                .color(this.color)
                .message(this.message)
                .maxWidth(this.maxWidth)
                .opacity(this.opacity)
                .build();
    }

}
