package dev.breezes.settlements.application.ui.sync;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface UiSessionValidator {

    Optional<String> validate(@Nonnull BaseVillager villager);

}
