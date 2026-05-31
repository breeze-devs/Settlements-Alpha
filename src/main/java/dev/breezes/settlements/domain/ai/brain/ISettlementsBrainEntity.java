package dev.breezes.settlements.domain.ai.brain;

import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.entities.SettlementsEntity;
import dev.breezes.settlements.domain.world.location.Location;

import javax.annotation.Nonnull;

public interface ISettlementsBrainEntity extends SettlementsEntity {

    IBrain getSettlementsBrain();

    INavigationManager<?> getNavigationManager();

    /**
     * Points the entity's head toward the given location for the current tick.
     */
    void lookAt(@Nonnull Location target);

}
