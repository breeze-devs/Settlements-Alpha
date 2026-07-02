package dev.breezes.settlements.infrastructure.minecraft.data.framework;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Base for datapack managers whose files each decode straight to one domain value keyed by an
 * id extracted from that value. Handles the later-file-replaces-earlier merge every such catalog
 * needs; subclasses only supply the value codec and the key extractor.
 */
public abstract class KeyedCatalogDataManager<K, T> extends CodecJsonDataManager<T> {

    private Map<K, T> byKey = Map.of();

    protected KeyedCatalogDataManager(@Nonnull String directory, @Nonnull Codec<T> codec) {
        super(directory, codec);
    }

    @Override
    protected final void onReloaded(@Nonnull Map<ResourceLocation, T> values) {
        LinkedHashMap<K, T> merged = new LinkedHashMap<>();
        for (T value : values.values()) {
            merged.put(this.keyOf(value), value);
        }

        this.byKey = Collections.unmodifiableMap(merged);
    }

    /**
     * Extracts the stable domain key used to deduplicate values across files.
     */
    protected abstract K keyOf(@Nonnull T value);

    protected final Optional<T> find(@Nonnull K key) {
        return Optional.ofNullable(this.byKey.get(key));
    }

    protected final Map<K, T> all() {
        return this.byKey;
    }

}
