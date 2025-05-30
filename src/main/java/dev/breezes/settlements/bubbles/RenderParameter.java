package dev.breezes.settlements.bubbles;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@Builder
@Getter
public class RenderParameter {

    private final LivingEntity entity;
    private final float partialTick;
    private final Player observer;
    private final MultiBufferSource buffer;
    private final PoseStack poseStack;
    private final EntityRenderDispatcher renderDispatcher;
    private final boolean hasVisualFocus;
    private final int packedLight;

}
