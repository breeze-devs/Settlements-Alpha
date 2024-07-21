package dev.breezes.settlements.models.conditions;

import lombok.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;


/**
 * Condition that evaluates if the game rule meets the expected value
 *
 * @param <T> the entity instance type, used to fetch game rule in the entity's world
 */
@Builder
public class GameRuleBooleanCondition<T extends Entity> implements IEntityCondition<T> {

    private final GameRules.Key<GameRules.BooleanValue> gameRule;
    private final boolean expectedValue;


    @Override
    public boolean test(@Nullable T entity) {
        return Optional.ofNullable(entity)
                .map(Entity::level)
                .map(Level::getGameRules)
                .map(gameRules -> gameRules.getBoolean(this.gameRule))
                .map(actualValue -> actualValue == this.expectedValue)
                .orElse(false);
    }

}
