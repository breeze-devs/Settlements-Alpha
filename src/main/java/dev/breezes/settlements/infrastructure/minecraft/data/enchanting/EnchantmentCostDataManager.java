package dev.breezes.settlements.infrastructure.minecraft.data.enchanting;

import dev.breezes.settlements.domain.enchanting.EnchantmentCostData;
import dev.breezes.settlements.domain.enchanting.EnchantmentCostDataCodec;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.KeyedCatalogDataManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;

public class EnchantmentCostDataManager extends KeyedCatalogDataManager<String, EnchantmentCostData> {

    private static final String DIRECTORY_PATH = "settlements/enchantments/costs";

    @Inject
    public EnchantmentCostDataManager() {
        super(DIRECTORY_PATH, EnchantmentCostDataCodec.CODEC);
    }

    @Override
    protected String label() {
        return "enchantment cost";
    }

    @Override
    protected String keyOf(@Nonnull EnchantmentCostData value) {
        return value.enchantmentId();
    }

    public Optional<EnchantmentCostData> getCost(@Nonnull String enchantmentId) {
        return this.find(enchantmentId);
    }

    public Collection<EnchantmentCostData> getAllCosts() {
        return this.all().values();
    }

}
