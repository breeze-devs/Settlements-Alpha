package dev.breezes.settlements.infrastructure.minecraft.data.animal;

import dev.breezes.settlements.domain.animal.ButcherableAnimalEntry;
import dev.breezes.settlements.domain.animal.ButcherableAnimalEntryCodec;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.KeyedCatalogDataManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;

public class ButcherableAnimalDataManager extends KeyedCatalogDataManager<ResourceLocation, ButcherableAnimalEntry> {

    private static final String DIRECTORY_PATH = "settlements/animals/butcherable";

    @Inject
    public ButcherableAnimalDataManager() {
        super(DIRECTORY_PATH, ButcherableAnimalEntryCodec.CODEC);
    }

    @Override
    protected String label() {
        return "butcherable animal";
    }

    @Override
    protected ResourceLocation keyOf(@Nonnull ButcherableAnimalEntry value) {
        return value.entityId();
    }

    public Collection<ButcherableAnimalEntry> getAllEntries() {
        return this.all().values();
    }

    public Optional<ButcherableAnimalEntry> findByEntityType(@Nonnull EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return this.find(id);
    }

}
