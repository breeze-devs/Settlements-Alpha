package dev.breezes.settlements.infrastructure.rendering.bubbles;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.rendering.highlight.SupplementaryEntityPass;
import dev.breezes.settlements.presentation.ui.ClientSurfaceSuppression;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@CustomLog
public class BubbleRenderClientEvents {

    @SubscribeEvent
    public static <T extends LivingEntity> void onEntityRender(RenderLivingEvent.Post<T, ? extends EntityModel<T>> event) {
        Minecraft minecraftClient = Minecraft.getInstance();
        Player player = minecraftClient.player;
        if (player == null) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof BaseVillager villager) || entity.isInvisibleTo(player)) {
            return;
        }

        // Bubble state advances by the partial tick handed to this callback, so it has to advance once per
        // frame rather than once per render call. A supplementary pass re-renders an entity vanilla already
        // drew this frame, which would age every bubble on that villager at double speed while it runs.
        if (event.getMultiBufferSource() instanceof SupplementaryEntityPass) {
            return;
        }

        BubbleManager bubbleManager = villager.getBubbleManager();
        bubbleManager.tick(event.getPartialTick());

        // Gating after the tick: expiry has to keep advancing while bubbles are hidden
        if (ClientSurfaceSuppression.isChatBubblesSuppressed(minecraftClient)) {
            return;
        }

        RenderParameter parameter = RenderParameter.builder()
                .entity(villager)
                .partialTick(event.getPartialTick())
                .observer(player)
                .buffer(event.getMultiBufferSource())
                .poseStack(event.getPoseStack())
                .renderDispatcher(minecraftClient.getEntityRenderDispatcher())
                .hasVisualFocus(event.getEntity() == minecraftClient.crosshairPickEntity)
                .packedLight(event.getPackedLight())
                .build();
        bubbleManager.render(parameter);
    }

}
