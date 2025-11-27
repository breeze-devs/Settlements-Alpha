package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.models.misc.ITickable;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Builder
@Getter
public class BehaviorDefinition<T extends ISettlementsBrainEntity> {

    private final ITickable preconditionCheckCooldown;
    private final ITickable behaviorCoolDown;

    @Singular
    private final List<ICondition<T>> preconditions;
    @Singular
    private final List<ICondition<T>> continueConditions;

    private final String loggerName;

}
