package dev.breezes.settlements.bubbles.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.bubbles.RenderParameter;
import dev.breezes.settlements.bubbles.canvas.BubbleInnerElement;
import dev.breezes.settlements.bubbles.canvas.BubbleRenderLayer;
import dev.breezes.settlements.bubbles.texture.AnimatedFrameTexture;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.Ticks;
import lombok.Builder;
import lombok.CustomLog;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

@CustomLog
public class BubbleShearSheepSpriteElement implements BubbleInnerElement {

    private static final AnimatedFrameTexture SHEARS_TEXTURE = AnimatedFrameTexture.builder()
            .id("bubble_shears")
            .texturePath("textures/bubble/anim_bubble_shears.png")
            .width(64)
            .height(32)
            .frameWidth(32)
            .frameCount(2)
            .build();

    private static final AnimatedFrameTexture SHEEP_TEXTURE = AnimatedFrameTexture.builder()
            .id("bubble_sheep")
            .texturePath("textures/bubble/anim_bubble_sheep.png")
            .width(64)
            .height(32)
            .frameWidth(32)
            .frameCount(2)
            .build();

    private static final int RENDER_SIZE = 16;
    private static final int BOUNDARY_WIDTH = 24;
    private static final int BOUNDARY_HEIGHT = 8;

    private final AtomicInteger frameIndexShears = new AtomicInteger(0);
    private final AtomicInteger frameIndexSheep = new AtomicInteger(0);

    private final Ticks frameDurationShears;
    private final Ticks frameDurationSheep;

    private final ITickable frameTickableShears;
    private final ITickable frameTickableSheep;

    @Builder
    private BubbleShearSheepSpriteElement(@Nonnull Ticks frameDurationShears, @Nonnull Ticks frameDurationSheep) {
        this.frameDurationShears = frameDurationShears;
        this.frameDurationSheep = frameDurationSheep;
        this.frameTickableShears = Tickable.of(frameDurationShears);
        this.frameTickableSheep = Tickable.of(frameDurationSheep);
    }

    @Override
    public void preRender(@Nonnull RenderParameter parameter) {
    }

    @Override
    public void render(@Nonnull RenderParameter parameter, @Nonnull PoseStack bubblePoseStack, @Nonnull Matrix4f bubblePose) {
        double deltaTick = parameter.getPartialTick();
        if (this.frameTickableShears.tickCheckAndReset(deltaTick)) {
            this.frameIndexShears.getAndUpdate(index -> (index + 1) % SHEARS_TEXTURE.getFrameCount());
        }
        if (this.frameTickableSheep.tickCheckAndReset(deltaTick)) {
            this.frameIndexSheep.getAndUpdate(index -> (index + 1) % SHEEP_TEXTURE.getFrameCount());
        }

        SHEARS_TEXTURE.drawFrame(this.frameIndexShears.get(), parameter.getBuffer(), bubblePose,
                -RENDER_SIZE, -RENDER_SIZE / 2.0F, RENDER_SIZE, RENDER_SIZE,
                BubbleRenderLayer.FOREGROUND_NORMAL, parameter.getPackedLight(), 1.0F);
        SHEEP_TEXTURE.drawFrame(this.frameIndexSheep.get(), parameter.getBuffer(), bubblePose,
                0, -RENDER_SIZE / 2.0F, RENDER_SIZE, RENDER_SIZE,
                BubbleRenderLayer.FOREGROUND_NORMAL, parameter.getPackedLight(), 1.0F);
    }

    @Override
    public BubbleBoundingBox getBoundingBox() {
        return BubbleBoundingBox.builder()
                .width(BOUNDARY_WIDTH)
                .height(BOUNDARY_HEIGHT)
                .build();
    }

    @Override
    public void adjustPoseStack(@Nonnull RenderParameter parameter, @Nonnull PoseStack poseStack) {
    }

    @Override
    public BubbleInnerElement copy() {
        return BubbleShearSheepSpriteElement.builder()
                .frameDurationShears(this.frameDurationShears)
                .frameDurationSheep(this.frameDurationSheep)
                .build();
    }

}
