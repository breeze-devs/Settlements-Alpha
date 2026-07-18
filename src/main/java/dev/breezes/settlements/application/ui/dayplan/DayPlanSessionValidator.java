package dev.breezes.settlements.application.ui.dayplan;

import dev.breezes.settlements.application.ui.sync.UiSessionValidator;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Optional;

@NoArgsConstructor(onConstructor_ = @Inject)
public final class DayPlanSessionValidator implements UiSessionValidator {

    @Override
    public Optional<String> validate(@Nonnull BaseVillager villager) {
        if (villager.getDayPlan() == null) {
            return Optional.of("ui.settlements.dayplan.no_plan");
        }
        return Optional.empty();
    }

}
