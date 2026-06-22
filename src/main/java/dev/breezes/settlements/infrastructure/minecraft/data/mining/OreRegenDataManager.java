package dev.breezes.settlements.infrastructure.minecraft.data.mining;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import dev.breezes.settlements.domain.mining.OreRegenEntry;
import dev.breezes.settlements.infrastructure.minecraft.blocks.DormantOreBlock;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class OreRegenDataManager extends SimpleJsonResourceReloadListener {

    private static final String DIRECTORY_PATH = "settlements/mining/ore_weights";
    private static final Gson GSON = new GsonBuilder().create();

    private List<OreRegenEntry> entries = List.of();
    private Map<OreRegenEntry, Double> stoneCandidates = Map.of();
    private Map<OreRegenEntry, Double> deepslateCandidates = Map.of();

    @Inject
    public OreRegenDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> resources,
                         @Nullable ResourceManager resourceManager,
                         @Nullable ProfilerFiller profiler) {
        Map<String, OreRegenEntry> parsed = new LinkedHashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> resource : resources.entrySet()) {
            ResourceLocation fileId = resource.getKey();
            try {
                RawOreRegenEntry raw = GSON.fromJson(resource.getValue(), RawOreRegenEntry.class);

                if (raw == null || raw.blockId == null || raw.blockId.isBlank()) {
                    log.warn("Skipping ore regen entry '{}': missing required 'block' field", fileId);
                    errorCount++;
                    continue;
                }
                if (raw.weight <= 0) {
                    log.warn("Skipping ore regen entry '{}': weight must be > 0, got {}", fileId, raw.weight);
                    errorCount++;
                    continue;
                }

                OreRegenEntry.HostFilter hostFilter = parseHostFilter(raw.rawHost, fileId.toString());
                OreRegenEntry normalised = OreRegenEntry.builder()
                        .blockId(raw.blockId)
                        .weight(raw.weight)
                        .host(hostFilter)
                        .build();

                if (parsed.containsKey(normalised.getBlockId())) {
                    log.warn("Duplicate ore regen entry for block '{}' in file '{}', overwriting",
                            normalised.getBlockId(), fileId);
                }
                parsed.put(normalised.getBlockId(), normalised);
            } catch (Exception e) {
                log.warn("Failed to parse ore regen entry from file '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        this.entries = List.copyOf(parsed.values());
        this.stoneCandidates = buildCandidatesForHost(DormantOreBlock.Host.STONE, this.entries);
        this.deepslateCandidates = buildCandidatesForHost(DormantOreBlock.Host.DEEPSLATE, this.entries);
        log.info("Loaded {} ore regen entries ({} errors)", this.entries.size(), errorCount);
    }

    /**
     * Exposed so unit tests can load JSON directly without a running Minecraft server.
     * Mirrors the ExcavateSubstrateYieldDataManager pattern.
     */
    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> resources) {
        this.apply(resources, null, null);
    }

    /**
     * Rolls a weighted random entry compatible with the requesting host.
     * <p>
     * Entries whose HostFilter is ANY are eligible for any host; otherwise the
     * entry's filter must match the requester. This keeps stone iron ore from
     * appearing in a deepslate mine, which would look like a worldgen glitch.
     *
     * @param requestingHost the host stratum of the dormant-ore block triggering the roll
     * @return the chosen entry, or empty if no compatible entries are loaded
     */
    public Optional<OreRegenEntry> rollForHost(@Nonnull DormantOreBlock.Host requestingHost) {
        Map<OreRegenEntry, Double> candidates = candidatesForHost(requestingHost);

        if (candidates.isEmpty()) {
            log.warn("No ore regen entries are compatible with host '{}' — check your datapack", requestingHost);
            return Optional.empty();
        }

        return Optional.of(RandomUtil.weightedChoice(candidates));
    }

    /**
     * Resolves the chosen entry's block id to a live BlockState.
     * <p>
     * This step touches {@link BuiltInRegistries} and cannot run in unit tests —
     * keep it separate from the pure roll logic so tests can stop at the entry level.
     * Returns empty if the id fails to resolve, mirroring the AIR-guard in ExcavateSubstrate.
     *
     * @param entry the entry returned by {@link #rollForHost}
     * @return the resolved block state, or empty if the block no longer exists
     */
    public Optional<BlockState> resolveBlockState(@Nonnull OreRegenEntry entry) {
        ResourceLocation blockLocation;
        try {
            blockLocation = ResourceLocation.parse(entry.getBlockId());
        } catch (Exception e) {
            log.warn("Ore regen entry has an unparseable block id '{}': {}", entry.getBlockId(), e.getMessage());
            return Optional.empty();
        }

        Block block = BuiltInRegistries.BLOCK.get(blockLocation);
        // BuiltInRegistries.BLOCK.get returns air for unknown ids, just like ITEM.
        if (block == Blocks.AIR) {
            log.warn("Ore regen entry references block '{}' which no longer resolves — skipping", entry.getBlockId());
            return Optional.empty();
        }

        return Optional.of(block.defaultBlockState());
    }

    private static Map<OreRegenEntry, Double> buildCandidatesForHost(@Nonnull DormantOreBlock.Host requestingHost,
                                                                     @Nonnull List<OreRegenEntry> entries) {
        Map<OreRegenEntry, Double> candidates = new LinkedHashMap<>();

        for (OreRegenEntry entry : entries) {
            if (supportsHost(entry.getHost(), requestingHost)) {
                candidates.put(entry, entry.getWeight());
            }
        }

        return Collections.unmodifiableMap(candidates);
    }

    private static boolean supportsHost(@Nullable OreRegenEntry.HostFilter entryFilter,
                                        @Nonnull DormantOreBlock.Host requestingHost) {
        return entryFilter == null
                || entryFilter == OreRegenEntry.HostFilter.ANY
                || switch (requestingHost) {
            case STONE -> entryFilter == OreRegenEntry.HostFilter.STONE;
            case DEEPSLATE -> entryFilter == OreRegenEntry.HostFilter.DEEPSLATE;
        };
    }

    private Map<OreRegenEntry, Double> candidatesForHost(@Nonnull DormantOreBlock.Host requestingHost) {
        return switch (requestingHost) {
            case STONE -> this.stoneCandidates;
            case DEEPSLATE -> this.deepslateCandidates;
        };
    }

    private static OreRegenEntry.HostFilter parseHostFilter(@Nullable String rawHost, @Nonnull String fileId) {
        if (rawHost == null || rawHost.isBlank()) {
            return OreRegenEntry.HostFilter.ANY;
        }

        return switch (rawHost.trim().toLowerCase(Locale.ROOT)) {
            case "stone" -> OreRegenEntry.HostFilter.STONE;
            case "deepslate" -> OreRegenEntry.HostFilter.DEEPSLATE;
            case "any" -> OreRegenEntry.HostFilter.ANY;
            default -> {
                log.warn("Unknown host filter '{}' in ore regen entry '{}' — defaulting to ANY", rawHost, fileId);
                yield OreRegenEntry.HostFilter.ANY;
            }
        };
    }

    public List<OreRegenEntry> getAllEntries() {
        return this.entries;
    }

    private static class RawOreRegenEntry {

        @SerializedName("block")
        private String blockId;

        @SerializedName("weight")
        private double weight;

        @SerializedName("host")
        private String rawHost;

    }

}
