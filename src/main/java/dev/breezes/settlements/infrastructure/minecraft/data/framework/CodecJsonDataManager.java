package dev.breezes.settlements.infrastructure.minecraft.data.framework;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic base for datapack-backed managers that decode every file with a single {@link Codec}
 */
@CustomLog
public abstract class CodecJsonDataManager<T> extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();

    private final Codec<T> codec;

    protected CodecJsonDataManager(@Nonnull String directory, @Nonnull Codec<T> codec) {
        super(GSON, directory);
        this.codec = codec;
    }

    @Override
    protected final void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                               @Nonnull ResourceManager resourceManager,
                               @Nonnull ProfilerFiller profiler) {
        this.reload(entries);
    }

    /**
     * Pure, server-free load path: decodes every entry with this manager's codec and hands the
     * successfully parsed values to {@link #onReloaded(Map)}. Exposed for direct test invocation.
     */
    @VisibleForTesting
    public final void reload(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        // Preserve entry iteration order: a later file overrides an earlier one's data by id
        Map<ResourceLocation, T> parsed = new LinkedHashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList()) {
            try {
                DataResult<T> result = this.codec.parse(JsonOps.INSTANCE, entry.getValue());
                Optional<T> value = result.result();
                if (value.isPresent()) {
                    parsed.put(entry.getKey(), value.get());
                } else {
                    result.resultOrPartial(message -> log.warn("Failed to parse {} entry '{}': {}", this.label(), entry.getKey(), message));
                    errorCount++;
                }
            } catch (Exception exception) {
                log.warn("Failed to parse {} entry '{}': {}", this.label(), entry.getKey(), exception.getMessage());
                errorCount++;
            }
        }

        this.onReloaded(parsed);
        log.info("Loaded {} {} entries ({} errors)", parsed.size(), this.label(), errorCount);
    }

    /**
     * Human-readable name for this manager's content, used in log messages (e.g. "craft catalog").
     */
    protected abstract String label();

    /**
     * Builds the manager's immutable snapshot / derived indexes from the successfully decoded files.
     */
    protected abstract void onReloaded(@Nonnull Map<ResourceLocation, T> values);

}
