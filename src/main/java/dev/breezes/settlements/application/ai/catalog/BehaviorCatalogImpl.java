package dev.breezes.settlements.application.ai.catalog;

import dev.breezes.settlements.domain.ai.catalog.BehaviorDisplayMetadata;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.di.catalog.BehaviorCatalogEntry;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ServerScope
public class BehaviorCatalogImpl implements IBehaviorCatalog {

    private final Map<BehaviorKey, BehaviorPlanningMetadata> descriptors;
    private final Map<BehaviorKey, BehaviorDisplayMetadata> displayInfos;
    private final Map<BehaviorKey, Supplier<IBehavior<BaseVillager>>> factories;

    @Inject
    BehaviorCatalogImpl(Set<BehaviorCatalogEntry> entries) {
        this.descriptors = entries.stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.descriptor().getKey(), BehaviorCatalogEntry::descriptor));
        this.displayInfos = entries.stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.descriptor().getKey(), BehaviorCatalogEntry::displayInfo));
        this.factories = entries.stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.descriptor().getKey(), BehaviorCatalogEntry::factory));
    }

    @Override
    public Optional<BehaviorPlanningMetadata> getDescriptor(BehaviorKey key) {
        return Optional.ofNullable(this.descriptors.get(key));
    }

    @Override
    public Optional<BehaviorDisplayMetadata> getDisplayInfo(BehaviorKey key) {
        return Optional.ofNullable(this.displayInfos.get(key));
    }

    @Override
    public Optional<IBehavior<BaseVillager>> createBehavior(BehaviorKey key) {
        Supplier<IBehavior<BaseVillager>> factory = this.factories.get(key);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory.get());
    }

    @Override
    public boolean exists(BehaviorKey key) {
        return this.descriptors.containsKey(key);
    }

    @Override
    public List<BehaviorPlanningMetadata> getAllDescriptors() {
        return List.copyOf(this.descriptors.values());
    }

}
