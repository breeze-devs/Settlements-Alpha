package dev.breezes.settlements.infrastructure.minecraft.data.traits;

import dev.breezes.settlements.domain.generation.model.profile.TraitDefinition;
import dev.breezes.settlements.domain.generation.model.profile.TraitDefinitionCodec;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.KeyedCatalogDataManager;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

public class TraitDefinitionDataManager extends KeyedCatalogDataManager<TraitId, TraitDefinition> implements TraitRegistry {

    private static final String DIRECTORY_PATH = "settlements/traits/definitions";

    @Inject
    public TraitDefinitionDataManager() {
        super(DIRECTORY_PATH, TraitDefinitionCodec.CODEC);
    }

    @Override
    protected String label() {
        return "trait definition";
    }

    @Override
    protected TraitId keyOf(@Nonnull TraitDefinition value) {
        return value.id();
    }

    @Override
    public Set<TraitId> allTraitIds() {
        return this.all().keySet();
    }

    @Override
    public Optional<TraitDefinition> byId(TraitId id) {
        return this.find(id);
    }

}
