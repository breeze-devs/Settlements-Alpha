package dev.breezes.settlements.models.actions.plan;

import dev.breezes.settlements.entities.villager.BaseVillager;

import java.util.Optional;

public class RepairIronGolemActionStep implements IActionStep<BaseVillager> {

    @Override
    public void tick(int delta) {

    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public Optional<IActionStep<BaseVillager>> getNextAction() {
        return Optional.empty();
    }

}
