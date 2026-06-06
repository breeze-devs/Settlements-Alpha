package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.SlotTargets;
import dev.breezes.settlements.domain.attachment.AttachmentContent;
import dev.breezes.settlements.domain.attachment.AttachmentProvider;
import dev.breezes.settlements.domain.attachment.RenderableAttachment;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.domain.presentation.ArmPose;
import dev.breezes.settlements.domain.presentation.AttachmentDisplayProfile;
import dev.breezes.settlements.domain.presentation.AttachmentDisplayProfileRegistry;
import dev.breezes.settlements.domain.presentation.SlotAnchor;
import dev.breezes.settlements.domain.presentation.SlotAnchorRegistry;
import dev.breezes.settlements.domain.presentation.Socket;
import dev.breezes.settlements.domain.presentation.SocketRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.SettlementsVillagerModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class AttachmentRenderLayer extends RenderLayer<BaseVillager, SettlementsVillagerModel<BaseVillager>> {

    private final SettlementsVillagerRenderer renderer;
    private final ItemInHandRenderer itemInHandRenderer;
    private final List<AttachmentProvider> attachmentProviders;
    private final SlotAnchorRegistry slotAnchorRegistry;
    private final SocketRegistry socketRegistry;
    private final AttachmentDisplayProfileRegistry displayProfileRegistry;

    public AttachmentRenderLayer(@Nonnull SettlementsVillagerRenderer renderer,
                                 @Nonnull ItemInHandRenderer itemInHandRenderer,
                                 @Nonnull Set<AttachmentProvider> attachmentProviders,
                                 @Nonnull SlotAnchorRegistry slotAnchorRegistry,
                                 @Nonnull SocketRegistry socketRegistry,
                                 @Nonnull AttachmentDisplayProfileRegistry displayProfileRegistry) {
        super(renderer);
        this.renderer = renderer;
        this.itemInHandRenderer = itemInHandRenderer;
        this.attachmentProviders = attachmentProviders.stream()
                .sorted(Comparator.comparingInt(AttachmentProvider::renderOrder)
                        .thenComparing(provider -> provider.getClass().getName()))
                .toList();
        this.slotAnchorRegistry = slotAnchorRegistry;
        this.socketRegistry = socketRegistry;
        this.displayProfileRegistry = displayProfileRegistry;
    }

    @Override
    public void render(@Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource buffer,
                       int packedLight,
                       @Nonnull BaseVillager villager,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTicks,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {
        AnimationFrame frame = this.renderer.currentFrame();
        for (AttachmentProvider provider : this.attachmentProviders) {
            for (RenderableAttachment attachment : provider.attachmentsFor(villager, partialTicks)) {
                this.renderAttachment(poseStack, buffer, packedLight, villager, attachment, frame);
            }
        }
    }

    private void renderAttachment(@Nonnull PoseStack poseStack,
                                  @Nonnull MultiBufferSource buffer,
                                  int packedLight,
                                  @Nonnull BaseVillager villager,
                                  @Nonnull RenderableAttachment attachment,
                                  @Nonnull AnimationFrame frame) {
        if (!frame.get(SlotTargets.visibility(attachment.slot()))) {
            return;
        }

        SlotAnchor anchor = this.slotAnchorRegistry.get(attachment.slot());
        // Resolve the socket from the model's current arm config
        ArmConfiguration config = this.getParentModel().getArmConfiguration();
        ArmPose handPose = EquipmentSlot.OFF_HAND.equals(attachment.slot()) ? config.left() : config.right();
        Socket socket = this.socketRegistry.get(anchor.socketFor(handPose));
        AttachmentDisplayProfile profile = this.displayProfileRegistry.get(attachment.slot(), attachment.category());

        poseStack.pushPose();
        if (socket.isInheritsBoneTransform()) {
            this.getParentModel().applyBoneTransform(poseStack, socket.getBone());
        }

        applyTransform(poseStack, socket.getLocalTranslation(), socket.getLocalRotation(), socket.getLocalScale());
        applyTransform(poseStack, profile.getTranslation(), profile.getRotation(), profile.getScale());
        applyTransform(
                poseStack,
                frame.get(SlotTargets.translation(attachment.slot())),
                frame.get(SlotTargets.rotation(attachment.slot())),
                frame.get(SlotTargets.scale(attachment.slot())));

        ItemDisplayContext displayContext = displayContextFor(attachment, anchor, profile, frame);
        this.renderContent(attachment.content(), villager, displayContext, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static ItemDisplayContext displayContextFor(@Nonnull RenderableAttachment attachment,
                                                        @Nonnull SlotAnchor anchor,
                                                        @Nonnull AttachmentDisplayProfile profile,
                                                        @Nonnull AnimationFrame frame) {
        ItemDisplayContext profileContext = profile.getDisplayContextOverride() == null
                ? anchor.getDefaultDisplayContext()
                : profile.getDisplayContextOverride();
        return frame.get(SlotTargets.displayContext(attachment.slot()), profileContext);
    }

    private void renderContent(@Nonnull AttachmentContent content,
                               @Nonnull BaseVillager villager,
                               @Nonnull ItemDisplayContext displayContext,
                               @Nonnull PoseStack poseStack,
                               @Nonnull MultiBufferSource buffer,
                               int packedLight) {
        switch (content) {
            case AttachmentContent.ItemContent itemContent -> this.itemInHandRenderer.renderItem(
                    villager,
                    itemContent.stack(),
                    displayContext,
                    false,
                    poseStack,
                    buffer,
                    packedLight);
            case AttachmentContent.ModelContent modelContent -> {
                // TODO: Resolve model ids through a client attachment-model registry once custom models exist.
            }
            case AttachmentContent.BillboardContent billboardContent -> {
                // TODO: Render camera-facing quads once transient visual props are introduced.
            }
        }
    }

    private static void applyTransform(@Nonnull PoseStack poseStack,
                                       @Nonnull Vec3 translation,
                                       @Nonnull Vector3f rotation,
                                       float scale) {
        poseStack.translate(translation.x, translation.y, translation.z);
        poseStack.mulPose(Axis.XP.rotation(rotation.x()));
        poseStack.mulPose(Axis.YP.rotation(rotation.y()));
        poseStack.mulPose(Axis.ZP.rotation(rotation.z()));
        poseStack.scale(scale, scale, scale);
    }

}
