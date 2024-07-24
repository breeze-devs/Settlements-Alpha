package dev.breezes.settlements.models.actions.state_machine;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.Ticks;

import javax.annotation.Nonnull;

// TODO: T should only extend smith-type villagers
public class RepairIronGolemOnceAction<T extends BaseVillager> extends TimedStateMachineAction<T> {

    public static final Ticks REPAIR_DURATION = Ticks.fromSeconds(1);

    public RepairIronGolemOnceAction(@Nonnull ICondition<T> originalTransitionCondition) {
        super(originalTransitionCondition, Tickable.of(REPAIR_DURATION));
    }

    @Override
    public void tickAction(int delta) {
        // TODO: display animation
        // TODO: heal golem
        // TODO: play sound
        // TODO: spawn particles
    }

}
