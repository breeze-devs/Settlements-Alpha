package dev.breezes.settlements.infrastructure.minecraft.data.farming.crops;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.farming.CultivationCropDefinition;
import dev.breezes.settlements.domain.farming.CultivationCropRegistry;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class CultivationCropDataManager extends SimpleJsonResourceReloadListener implements CultivationCropRegistry {

    private static final String DIRECTORY_PATH = "settlements/cultivation_crops";
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private Map<ResourceLocation, CultivationCropDefinition> bySeedItem = Map.of();
    private Map<ResourceLocation, CultivationCropDefinition> byCropBlock = Map.of();

    @Inject
    public CultivationCropDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<ResourceLocation, CultivationCropDefinition> parsedBySeed = new LinkedHashMap<>();
        Map<ResourceLocation, CultivationCropDefinition> parsedByCrop = new LinkedHashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                CultivationCropFile file = GSON.fromJson(entry.getValue(), CultivationCropFile.class);
                CultivationCropDefinition definition = parseFile(file, entry.getKey());
                parsedBySeed.put(definition.seedItem(), definition);
                parsedByCrop.put(definition.cropBlock(), definition);
            } catch (Exception exception) {
                log.warn("Failed to parse cultivation crop file '{}': {}", entry.getKey(), exception.getMessage());
                errorCount++;
            }
        }

        this.bySeedItem = Map.copyOf(parsedBySeed);
        this.byCropBlock = Map.copyOf(parsedByCrop);
        log.info("Loaded {} cultivation crop definitions ({} errors)", this.byCropBlock.size(), errorCount);
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

    private static CultivationCropDefinition parseFile(CultivationCropFile file, ResourceLocation fileId) {
        if (file == null) {
            throw new IllegalArgumentException("parsed entry was null");
        }

        ResourceLocation seedItem = parseId(file.seedItem, "seed_item");
        ResourceLocation displayItem = parseId(file.displayItem, "display_item");
        ResourceLocation cropBlock = parseId(file.cropBlock, "crop_block");

        validateItem(seedItem, "seed_item", fileId);
        validateItem(displayItem, "display_item", fileId);
        validateCropBlock(cropBlock, fileId);

        return new CultivationCropDefinition(seedItem, displayItem, cropBlock);
    }

    private static ResourceLocation parseId(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("missing " + fieldName);
        }
        return ResourceLocation.parse(raw);
    }

    private static void validateItem(ResourceLocation itemId, String fieldName, ResourceLocation fileId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("unknown " + fieldName + " '" + itemId + "' in " + fileId);
        }
    }

    private static void validateCropBlock(ResourceLocation blockId, ResourceLocation fileId) {
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        if (block == Blocks.AIR) {
            throw new IllegalArgumentException("unknown crop_block '" + blockId + "' in " + fileId);
        }
        if (!(block instanceof CropBlock)) {
            throw new IllegalArgumentException("crop_block '" + blockId + "' in " + fileId + " is not a CropBlock");
        }
    }

    private static final class CultivationCropFile {
        private String seedItem;
        private String displayItem;
        private String cropBlock;
    }

}
