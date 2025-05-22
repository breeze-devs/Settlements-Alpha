package dev.breezes.settlements.bubbles;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

// TODO: https://github.com/Mrbysco/NotableBubbleText/blob/bb245e78bcfd1edebe442cbb025c1ad91dfe94b9/src/main/java/com/mrbysco/nbt/command/BubbleCommands.java#L59-L68
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

        // Perform rendering
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

        BubbleManager bubbleManager = villager.getBubbleManager();
        bubbleManager.tick(event.getPartialTick());
        bubbleManager.render(parameter);
    }

}
