package dev.breezes.settlements.bubbles.canvas;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.bubbles.RenderParameter;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

@Builder
@Getter
public class BubbleBoundaryElement {

    private static final ResourceLocation BUBBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "textures/bubble/speech_bubble.png");

    public static RenderType BUBBLE = RenderType.create("settlements:speech_bubble",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256,
            false, false,
            RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(BUBBLE_TEXTURE, false, false))
                    .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(false));

    private BubbleInnerElement innerElement;

    @Builder.Default
    private float opacity = 0.8F;

    public void render(@Nonnull RenderParameter parameter) {
        PoseStack poseStack = parameter.getPoseStack();
        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();

        Entity entity = parameter.getEntity();
        poseStack.translate(0.0D, entity.getDimensions(entity.getPose()).height() + (0.5 + this.getNameOffset(parameter)), 0D);
        this.innerElement.adjustPoseStack(parameter, poseStack);
        poseStack.mulPose(parameter.getRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        this.innerElement.preRender(parameter);


        // Render border
        this.renderBubble(parameter, pose);

        // Render inner elements
        this.innerElement.render(parameter, poseStack, pose);

        poseStack.popPose();
    }

    private void renderBubble(@Nonnull RenderParameter parameter, @Nonnull Matrix4f pose) {
        VertexConsumer buffer = parameter.getBuffer().getBuffer(BUBBLE);

        int light = parameter.getPackedLight();

        int imageSize = 32; // TODO: texture size edge pixel count (square)
        int edgeSize = 6; // TODO: edge size in pixels

        BubbleBoundingBox boundingBox = this.innerElement.getBoundingBox();
        float fullWidth = boundingBox.getWidth() + edgeSize * 2;
        float fullHeight = boundingBox.getHeight() + edgeSize * 2;

        float xStart = -fullWidth / 2F;
        float yStart = -fullHeight / 2F;

        //Render Top left corner of bubble
        drawTexture(pose, buffer, xStart, yStart, BubbleRenderLayer.BACKGROUND_NORMAL, 0, 0, 6, 6, 6, 6, imageSize, imageSize, light);
        //Render Top right corner of bubble
        drawTexture(pose, buffer, xStart + fullWidth - edgeSize, yStart, BubbleRenderLayer.BACKGROUND_NORMAL, 9, 0, 6, 6, 6, 6, imageSize, imageSize, light);
        //Render Bottom left corner of bubble
        drawTexture(pose, buffer, xStart, yStart + fullHeight - edgeSize, BubbleRenderLayer.BACKGROUND_NORMAL, 0, 9, 6, 6, 6, 6, imageSize, imageSize, light);
        //Render Bottom right corner of bubble
        drawTexture(pose, buffer, xStart + fullWidth - edgeSize, yStart + fullHeight - edgeSize, BubbleRenderLayer.BACKGROUND_NORMAL, 9, 9, 6, 6, 6, 6, imageSize, imageSize, light);

        //Render the top center of the bubble
        drawTexture(pose, buffer, xStart + edgeSize, yStart, BubbleRenderLayer.BACKGROUND_NORMAL, 7, 0, 1, 6, boundingBox.getWidth(), 6, imageSize, imageSize, light);
        //Render the left middle of the bubble
        drawTexture(pose, buffer, xStart, yStart + edgeSize, BubbleRenderLayer.BACKGROUND_NORMAL, 0, 7, 6, 1, 6, boundingBox.getHeight(), imageSize, imageSize, light);
        //Render the right middle of the bubble
        drawTexture(pose, buffer, xStart + fullWidth - edgeSize, yStart + edgeSize, BubbleRenderLayer.BACKGROUND_NORMAL, 9, 7, 6, 1, 6, boundingBox.getHeight(), imageSize, imageSize, light);
        //Render the bottom center of the bubble
        drawTexture(pose, buffer, xStart + edgeSize, yStart + fullHeight - edgeSize, BubbleRenderLayer.BACKGROUND_NORMAL, 7, 9, 1, 6, boundingBox.getWidth(), 6, imageSize, imageSize, light);

        //Render the center of the bubble
        drawTexture(pose, buffer, xStart + edgeSize, yStart + edgeSize, BubbleRenderLayer.BACKGROUND_NORMAL, 7, 7, 1, 1, boundingBox.getWidth(), boundingBox.getHeight(), imageSize, imageSize, light);

        //Render the tail of the bubble based on the above uv positions offset using the poseStack
        int tailTextureSize = 6;
        drawTexture(pose, buffer, xStart + fullWidth / 2F - tailTextureSize / 2F, yStart + fullHeight - 1.5F, BubbleRenderLayer.BACKGROUND_HIGH, 0, 16, 6, 5, 5, 5, imageSize, imageSize, light);

        if (parameter.getBuffer() instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(BUBBLE);
        }
    }

    private void drawTexture(Matrix4f pose, VertexConsumer vertexConsumer,
                             float x, float y,
                             @Nonnull BubbleRenderLayer layer,
                             float u, float v, float uWidth, float vHeight,
                             float renderWidth, float renderHeight,
                             float textureWidth, float textureHeight, int light) {
        float widthRatio = 1 / textureWidth;
        float heightRatio = 1 / textureHeight;

        vertexConsumer.addVertex(pose, x, y + renderHeight, layer.getZIndex())
                .setColor(1.0F, 1.0F, 1.0F, this.opacity)
                .setUv(u * widthRatio, (v + vHeight) * heightRatio)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(pose, x + renderWidth, y + renderHeight, layer.getZIndex())
                .setColor(1.0F, 1.0F, 1.0F, this.opacity)
                .setUv((u + uWidth) * widthRatio, (v + vHeight) * heightRatio)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(pose, x + renderWidth, y, layer.getZIndex())
                .setColor(1.0F, 1.0F, 1.0F, this.opacity)
                .setUv((u + uWidth) * widthRatio, v * heightRatio)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY);
        vertexConsumer.addVertex(pose, x, y, layer.getZIndex())
                .setColor(1.0F, 1.0F, 1.0F, this.opacity)
                .setUv(u * widthRatio, v * heightRatio)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY);
    }

    protected double getNameOffset(@Nonnull RenderParameter renderParameter) {
        Entity entity = renderParameter.getEntity();

        // Check if entity has custom name and should be displayed
        if (!entity.shouldShowName() && !(renderParameter.isHasVisualFocus() && entity.hasCustomName())) {
            return 0;
        }

        Vec3 vec3 = entity.getAttachments()
                .getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(renderParameter.getPartialTick()));
        if (vec3 == null) {
            return 0;
        }

        return vec3.y * 0.275D;
    }

    public BubbleBoundaryElement copy() {
        return BubbleBoundaryElement.builder()
                .innerElement(this.innerElement.copy())
                .opacity(this.opacity)
                .build();
    }

}
