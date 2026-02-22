package dev.breezes.settlements.domain.entities;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;

import dev.breezes.settlements.domain.inventory.VillagerInventory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ISettlementsVillager extends ISettlementsBrainEntity {

    UUID getUUID();

    // Inventory
    VillagerInventory getSettlementsInventory();

    void pickUp(ItemEntity itemEntity);

    // Held item management
    Optional<ItemStack> getHeldItem();

    void setHeldItem(@Nonnull ItemStack itemStack);

    void clearHeldItem();

    // Navigation
    INavigationManager<BaseVillager> getNavigationManager();

    @Override
    BaseVillager getMinecraftEntity();

    static List<Class<? extends Mob>> getEnemyClasses() {
        return List.of(
                Zombie.class,
                Pillager.class,
                Vindicator.class,
                Vex.class,
                Witch.class,
                Evoker.class,
                Illusioner.class,
                Ravager.class);
    }

}
