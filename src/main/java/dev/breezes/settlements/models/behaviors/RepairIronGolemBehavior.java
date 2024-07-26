package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

@CustomLog
public class RepairIronGolemBehavior extends AbstractBehavior<BaseVillager> {

    // TODO: implement the conditions
    // TODO: forgo framework design for now, just implement the behavior directly
    // TODO: we can refactor later
//    private static final IEntityCondition<BaseVillager> nearbyDamagedGolemExistsCondition = null;

//    private IActionPlan actionPlan = ActionPlan.builder()
//            .action(Actions.REPAIR_IRON_GOLEM)
//            .build();

    public RepairIronGolemBehavior() {
//        super(log, List.of(nearbyDamagedGolemExistsCondition));
        super(log, List.of(), List.of(), null, null, null);
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        IronGolem targetToRepair = null;

    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager entity) {

    }

}
