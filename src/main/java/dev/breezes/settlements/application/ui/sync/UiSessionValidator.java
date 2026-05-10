package dev.breezes.settlements.application.ui.sync;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;
import java.util.Optional;

@FunctionalInterface
public interface UiSessionValidator {

    UiSessionValidator ALWAYS_VALID = villager -> Optional.empty();

    Optional<String> validate(@Nonnull BaseVillager villager);

}
