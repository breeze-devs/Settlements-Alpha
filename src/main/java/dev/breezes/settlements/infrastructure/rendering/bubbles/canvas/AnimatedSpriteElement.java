package dev.breezes.settlements.infrastructure.rendering.bubbles.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.infrastructure.rendering.bubbles.RenderParameter;
import dev.breezes.settlements.infrastructure.rendering.bubbles.texture.AnimatedFrameTexture;
import lombok.Builder;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

public class AnimatedSpriteElement implements BubbleInnerElement {

    private static final int RENDER_SIZE = 16;
    private static final int BOUNDARY_SIZE = 12;

    private final AnimatedFrameTexture texture;
    private final Ticks frameDuration;

    private final AtomicInteger frameIndex;
    private final ITickable frameTickable;

    @Builder
    public AnimatedSpriteElement(@Nonnull AnimatedFrameTexture texture,
                                 @Nonnull Ticks frameDuration,
                                 @Nullable AtomicInteger frameIndex,
                                 @Nullable ITickable frameTickable) {
        this.texture = texture;
        this.frameDuration = frameDuration;
        this.frameIndex = frameIndex != null ? frameIndex : new AtomicInteger(0);
        // Animation state stays per-instance so concurrent bubbles do not accidentally synchronize.
        this.frameTickable = frameTickable != null ? frameTickable : Tickable.of(frameDuration);
    }

    @Override
    public void preRender(@Nonnull RenderParameter parameter) {
    }

    @Override
    public void render(@Nonnull RenderParameter parameter, @Nonnull PoseStack bubblePoseStack, @Nonnull Matrix4f bubblePose) {
        if (this.texture.getFrameCount() > 1
                && this.frameDuration.getTicks() > 0
                && this.frameTickable.tickCheckAndReset(parameter.getPartialTick())) {
            this.frameIndex.getAndUpdate(index -> (index + 1) % this.texture.getFrameCount());
        }

        this.texture.drawFrame(
                this.frameIndex.get(),
                parameter.getBuffer(),
                bubblePose,
                -RENDER_SIZE / 2.0F,
                -RENDER_SIZE / 2.0F,
                RENDER_SIZE,
                RENDER_SIZE,
                BubbleRenderLayer.FOREGROUND_NORMAL,
                parameter.getPackedLight(),
                1.0F);
    }

    @Override
    public BubbleBoundingBox getBoundingBox() {
        return BubbleBoundingBox.builder()
                .width(BOUNDARY_SIZE)
                .height(BOUNDARY_SIZE)
                .build();
    }

    @Override
    public void adjustPoseStack(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack) {
    }

    @Override
    public BubbleInnerElement copy() {
        return AnimatedSpriteElement.builder()
                .texture(this.texture)
                .frameDuration(this.frameDuration)
                .build();
    }

}
