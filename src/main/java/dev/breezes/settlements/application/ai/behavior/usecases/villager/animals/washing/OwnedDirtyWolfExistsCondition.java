package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.washing;

import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.OwnedWolfExistsCondition;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;

import javax.annotation.Nullable;
import java.util.List;

public class OwnedDirtyWolfExistsCondition implements ICondition<BaseVillager> {

    private final OwnedWolfExistsCondition delegate;

    public OwnedDirtyWolfExistsCondition(int scanRangeHorizontal, int scanRangeVertical) {
        this.delegate = new OwnedWolfExistsCondition(scanRangeHorizontal, scanRangeVertical, SettlementsWolf::isDirty);
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        return this.delegate.test(villager);
    }

    public List<SettlementsWolf> getTargets() {
        return this.delegate.getTargets();
    }

}
