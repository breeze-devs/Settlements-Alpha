package dev.breezes.settlements.entities.villager;

import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.models.navigation.INavigationManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;
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

    static List<Class<? extends Mob>> getEnemyClasses() {
        return List.of(
                Zombie.class,
                Pillager.class,
                Vindicator.class,
                Vex.class,
                Witch.class,
                Evoker.class,
                Illusioner.class,
                Ravager.class
        );
    }

}
