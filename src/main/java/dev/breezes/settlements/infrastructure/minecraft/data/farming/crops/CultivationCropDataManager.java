package dev.breezes.settlements.infrastructure.minecraft.data.farming.crops;

import dev.breezes.settlements.domain.farming.CultivationCropDefinition;
import dev.breezes.settlements.domain.farming.CultivationCropDefinitionCodec;
import dev.breezes.settlements.domain.farming.CultivationCropRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.CodecJsonDataManager;
import jakarta.inject.Inject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class CultivationCropDataManager extends CodecJsonDataManager<CultivationCropDefinition> implements CultivationCropRegistry {

    private static final String DIRECTORY_PATH = "settlements/cultivation_crops";

    private Map<ResourceLocation, CultivationCropDefinition> bySeedItem = Map.of();
    private Map<ResourceLocation, CultivationCropDefinition> byCropBlock = Map.of();

    @Inject
    public CultivationCropDataManager() {
        super(DIRECTORY_PATH, CultivationCropDefinitionCodec.CODEC);
    }

    @Override
    protected String label() {
        return "cultivation crop definition";
    }

    @Override
    protected void onReloaded(@Nonnull Map<ResourceLocation, CultivationCropDefinition> values) {
        Map<ResourceLocation, CultivationCropDefinition> parsedBySeed = new LinkedHashMap<>();
        Map<ResourceLocation, CultivationCropDefinition> parsedByCrop = new LinkedHashMap<>();

        for (CultivationCropDefinition definition : values.values()) {
            parsedBySeed.put(definition.seedItem(), definition);
            parsedByCrop.put(definition.cropBlock(), definition);
        }

        this.bySeedItem = Map.copyOf(parsedBySeed);
        this.byCropBlock = Map.copyOf(parsedByCrop);
    }

    @Override
    public Optional<CultivationCropDefinition> resolveBySeedItem(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Optional.ofNullable(this.bySeedItem.get(itemId));
    }

    @Override
    public Optional<CultivationCropDefinition> resolveByCropBlock(@Nonnull ResourceLocation cropBlockId) {
        return Optional.ofNullable(this.byCropBlock.get(cropBlockId));
    }

    @Override
    public Collection<CultivationCropDefinition> all() {
        return Collections.unmodifiableCollection(this.bySeedItem.values());
    }

}
