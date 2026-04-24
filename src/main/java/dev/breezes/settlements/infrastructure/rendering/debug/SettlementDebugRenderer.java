package dev.breezes.settlements.infrastructure.rendering.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.domain.generation.model.layout.RoadType;
import dev.breezes.settlements.infrastructure.network.features.debug.packet.ClientBoundSettlementDebugPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nonnull;

public final class SettlementDebugRenderer {

    private SettlementDebugRenderer() {
    }

    public static void render(@Nonnull RenderLevelStageEvent event,
                              @Nonnull SettlementDebugOverlayState settlementDebugOverlayState) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        settlementDebugOverlayState.get().ifPresent(packet -> renderOverlay(event, packet));
    }

    private static void renderOverlay(@Nonnull RenderLevelStageEvent event,
                                      @Nonnull ClientBoundSettlementDebugPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gameRenderer == null || minecraft.gameRenderer.getMainCamera() == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        renderBoundingBox(poseStack, lines, packet.settlementBox(), 1.0f, 1.0f, 1.0f, 1.0f);

        for (ClientBoundSettlementDebugPacket.BuildingBox building : packet.buildings()) {
            renderBoundingBox(poseStack, lines, building.box(), 0.2f, 1.0f, 0.2f, 0.8f);
        }

        for (ClientBoundSettlementDebugPacket.RoadBox road : packet.roads()) {
            float[] color = colorFor(road.roadType());
            renderBoundingBox(poseStack, lines, road.box(), color[0], color[1], color[2], color[3]);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private static void renderBoundingBox(@Nonnull PoseStack poseStack,
                                          @Nonnull VertexConsumer lines,
                                          @Nonnull BoundingBox boundingBox,
                                          float red,
                                          float green,
                                          float blue,
                                          float alpha) {
        AABB aabb = new AABB(
                boundingBox.minX(),
                boundingBox.minY(),
                boundingBox.minZ(),
                boundingBox.maxX() + 1.0D,
                boundingBox.maxY() + 1.0D,
                boundingBox.maxZ() + 1.0D);
        LevelRenderer.renderLineBox(poseStack, lines, aabb, red, green, blue, alpha);
    }

    @Nonnull
    private static float[] colorFor(@Nonnull RoadType roadType) {
        return switch (roadType) {
            case MAIN -> new float[]{1.0f, 0.9f, 0.0f, 0.7f};
            case SECONDARY -> new float[]{1.0f, 0.55f, 0.0f, 0.7f};
            case SIDE -> new float[]{0.6f, 0.6f, 0.6f, 0.5f};
        };
    }

}
