package dev.breezes.settlements.bubbles.texture;

import dev.breezes.settlements.bubbles.canvas.BubbleRenderLayer;
import dev.breezes.settlements.util.MathUtil;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

@SuperBuilder
@Getter
public class AnimatedFrameTexture extends Texture {

    private int frameWidth;
    private int frameCount;

    public void drawFrame(int frameIndex, @Nonnull MultiBufferSource bufferSource, @Nonnull Matrix4f pose,
                          float x, float y, float renderWidth, float renderHeight,
                          @Nonnull BubbleRenderLayer layer, int light, float opacity) {
        if (!MathUtil.inRange(frameIndex, 0, this.frameCount - 1)) {
            throw new IllegalArgumentException("Frame index out of range: " + frameIndex);
        }

        int frameStartU = frameIndex * this.frameWidth;
        super.draw(bufferSource, pose,
                this.getWidth(), this.getHeight(),
                x, y, renderWidth, renderHeight,
                frameStartU, 0, this.frameWidth, this.getHeight(),
                layer, light, opacity);
    }

}
