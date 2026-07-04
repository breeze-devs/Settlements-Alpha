package dev.breezes.settlements.infrastructure.minecraft.entities.wolves.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.domain.animal.PetSquish;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.world.entity.animal.Wolf;

import javax.annotation.Nonnull;

/**
 * Renders a {@link SettlementsWolf} identically to a vanilla wolf, adding a transient squash-and-stretch
 * when the wolf is petted.
 * <p>
 * The squish is a whole-model {@link PoseStack} scale layered on top of vanilla's transforms, so it needs
 * no model subclass and never touches the hitbox. Mirrors the Settlements cat renderer.
 */
public class SettlementsWolfRenderer extends WolfRenderer {

    public SettlementsWolfRenderer(@Nonnull EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(@Nonnull Wolf wolf, @Nonnull PoseStack poseStack, float partialTickTime) {
        // Preserve any vanilla scaling; our squish multiplies on top of it.
        super.scale(wolf, poseStack, partialTickTime);

        if (wolf instanceof SettlementsWolf settlementsWolf) {
            PetSquish.Factors squish = settlementsWolf.getPetSquishFactors(partialTickTime);
            poseStack.scale(squish.xz(), squish.y(), squish.xz());
        }
    }

}
