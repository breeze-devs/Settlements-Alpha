package dev.breezes.settlements.infrastructure.minecraft.data.farming.crops;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

/**
 * Client-side mirror of which item ids the server's cultivation crop registry accepts as seeds.
 * <p>
 * The registry itself is datapack-backed and does not exist client-side, so this holds a plain item
 * id set synced from the server rather than resolving crop definitions locally.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class CultivationSeedSetClientProjection implements ClientSessionResettable {

    private Set<ResourceLocation> acceptedSeedItemIds = Set.of();

    public void applySnapshot(@Nonnull Collection<ResourceLocation> seedItemIds) {
        this.acceptedSeedItemIds = Set.copyOf(seedItemIds);
    }

    public boolean isAcceptedSeed(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return this.acceptedSeedItemIds.contains(itemId);
    }

    @Override
    public void onClientSessionEnded() {
        this.acceptedSeedItemIds = Set.of();
    }

}
