package dev.breezes.settlements.infrastructure.minecraft.data.enchanting;

import dev.breezes.settlements.domain.enchanting.SpecializationProfile;
import dev.breezes.settlements.domain.enchanting.SpecializationProfileCodec;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.KeyedCatalogDataManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

public class SpecializationDataManager extends KeyedCatalogDataManager<String, SpecializationProfile> {

    private static final String DIRECTORY_PATH = "settlements/specializations";

    @Inject
    public SpecializationDataManager() {
        super(DIRECTORY_PATH, SpecializationProfileCodec.CODEC);
    }

    @Override
    protected String label() {
        return "specialization";
    }

    @Override
    protected String keyOf(@Nonnull SpecializationProfile value) {
        return value.id();
    }

    public Optional<SpecializationProfile> getProfile(@Nonnull String id) {
        return this.find(id);
    }

}
