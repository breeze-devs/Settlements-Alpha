package dev.breezes.settlements.infrastructure.minecraft.entities.cats.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.domain.animal.PetSquish;
import dev.breezes.settlements.infrastructure.minecraft.entities.cats.SettlementsCat;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.animal.Cat;

import javax.annotation.Nonnull;

/**
 * Renders a {@link SettlementsCat} identically to a vanilla cat, adding a transient squash-and-stretch
 * when the cat is petted.
 * <p>
 * The squish is a whole-model {@link PoseStack} scale layered on top of vanilla's transforms, so it needs
 * no model subclass and never touches the hitbox. Extending {@link CatRenderer} keeps vanilla texture
 * resolution and the collar layer for free; the chonk feature will later grow this class with a model swap.
 */
public class SettlementsCatRenderer extends CatRenderer {

    public SettlementsCatRenderer(@Nonnull EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(@Nonnull Cat cat, @Nonnull PoseStack poseStack, float partialTickTime) {
        // Preserve vanilla scaling (the fixed 0.8 cat shrink); our squish multiplies on top of it.
        super.scale(cat, poseStack, partialTickTime);

        if (cat instanceof SettlementsCat settlementsCat) {
            PetSquish.Factors squish = settlementsCat.getPetSquishFactors(partialTickTime);
            poseStack.scale(squish.xz(), squish.y(), squish.xz());
        }
    }

}
