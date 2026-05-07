package dev.breezes.settlements.di.catalog;

import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorUiDisplayInfo;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;

import java.util.function.Supplier;

/**
 * Wiring-layer pairing of a behavior's planning metadata with its Dagger-managed factory.
 * <p>
 * Consumed exclusively by {@code BehaviorCatalogImpl} at component creation time to build
 * the catalog's internal index. Neither the metadata nor the factory escape into domain code.
 * <p>
 * One {@code BehaviorCatalogEntry} per behavior variant — parameterized variants (e.g. breed_pigs vs.
 * breed_chickens) are separate entries with separate factories even if they share an implementation class.
 */
@Builder
public record BehaviorCatalogEntry(
        BehaviorPlanningMetadata descriptor,
        BehaviorUiDisplayInfo displayInfo,
        Supplier<IBehavior<BaseVillager>> factory
) {
}
