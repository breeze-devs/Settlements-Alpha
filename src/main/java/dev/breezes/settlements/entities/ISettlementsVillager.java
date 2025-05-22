package dev.breezes.settlements.entities;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.navigation.INavigationManager;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

public interface ISettlementsVillager extends ISettlementsBrainEntity {

    UUID getUUID();

    // Held item management
    Optional<ItemStack> getHeldItem();

    void setHeldItem(@Nonnull ItemStack itemStack);

    void clearHeldItem();

    // Navigation
    INavigationManager<BaseVillager> getNavigationManager();

    @Override
    BaseVillager getMinecraftEntity();

}
