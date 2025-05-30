package dev.breezes.settlements.bubbles.texture;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.bubbles.canvas.BubbleRenderLayer;
import dev.breezes.settlements.util.ResourceLocationUtil;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

@SuperBuilder
@Getter
public class Texture {

    private final String id;
    private final String texturePath; // textures/bubble/speech_bubble.png

    private int width;
    private int height;

    public ResourceLocation getResourceLocation() {
        return ResourceLocationUtil.mod(this.texturePath);
    }

    public RenderType getBubbleRenderType() {
        return RenderType.create(
                this.getFullId(),
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setTextureState(new RenderStateShard.TextureStateShard(this.getResourceLocation(), false, false))
                        .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(false));

    }

    public String getFullId() {
        return SettlementsMod.MOD_ID + ":" + this.id;
    }

    public void draw(@Nonnull MultiBufferSource bufferSource, @Nonnull Matrix4f pose,
                     float x, float y, float renderWidth, float renderHeight,
                     @Nonnull BubbleRenderLayer layer, int light, float opacity) {
        this.draw(bufferSource, pose,
                this.width, this.height,
                x, y, renderWidth, renderHeight,
                0, 0, this.width, this.height,
                layer, light, opacity);
    }

    protected void draw(@Nonnull MultiBufferSource bufferSource, @Nonnull Matrix4f pose,
                        float textureWidth, float textureHeight,
                        float x, float y, float renderWidth, float renderHeight,
                        float u, float v, float uWidth, float vHeight,
                        @Nonnull BubbleRenderLayer layer, int light, float opacity) {
        float widthRatio = 1.0F / textureWidth;
        float heightRatio = 1.0F / textureHeight;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(this.getBubbleRenderType());
        vertexConsumer.addVertex(pose, x, y + renderHeight, layer.getZIndex())
                .setColor(1.0F, 1.0F, 1.0F, opacity)
                .setUv(u * widthRatio, (v + vHeight) * heightRatio)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(pose, x + renderWidth, y + renderHeight, layer.getZIndex())
                .setColor(1.0F, 1.0F, 1.0F, opacity)
                .setUv((u + uWidth) * widthRatio, (v + vHeight) * heightRatio)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(pose, x + renderWidth, y, layer.getZIndex())
                .setColor(1.0F, 1.0F, 1.0F, opacity)
                .setUv((u + uWidth) * widthRatio, v * heightRatio)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(pose, x, y, layer.getZIndex())
                .setColor(1.0F, 1.0F, 1.0F, opacity)
                .setUv(u * widthRatio, v * heightRatio)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY);

        if (bufferSource instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(this.getBubbleRenderType());
        }
    }

}
