package dev.breezes.settlements.application.ui.behavior.snapshot;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;

public interface IBehaviorInfoProvider {

    BehaviorRuntimeInformation getBehaviorRuntimeInformation(@Nonnull BaseVillager villager);

}
