package dev.breezes.settlements.infrastructure.minecraft.data.mining;

import dev.breezes.settlements.domain.mining.OreRegenEntry;
import dev.breezes.settlements.domain.mining.OreRegenEntryCodec;
import dev.breezes.settlements.infrastructure.minecraft.blocks.DormantOreBlock;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.CodecJsonDataManager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class OreRegenDataManager extends CodecJsonDataManager<OreRegenEntry> {

    private static final String DIRECTORY_PATH = "settlements/mining/ore_weights";

    private List<OreRegenEntry> entries = List.of();
    private Map<OreRegenEntry, Double> stoneCandidates = Map.of();
    private Map<OreRegenEntry, Double> deepslateCandidates = Map.of();

    @Inject
    public OreRegenDataManager() {
        super(DIRECTORY_PATH, OreRegenEntryCodec.CODEC);
    }

    @Override
    protected String label() {
        return "ore regen entry";
    }

    @Override
    protected void onReloaded(@Nonnull Map<ResourceLocation, OreRegenEntry> values) {
        // A later file overrides an earlier one that declares the same block
        Map<ResourceLocation, OreRegenEntry> deduplicatedByBlock = new LinkedHashMap<>();
        for (OreRegenEntry entry : values.values()) {
            deduplicatedByBlock.put(entry.getBlockId(), entry);
        }

        this.entries = List.copyOf(deduplicatedByBlock.values());
        this.stoneCandidates = buildCandidatesForHost(DormantOreBlock.Host.STONE, this.entries);
        this.deepslateCandidates = buildCandidatesForHost(DormantOreBlock.Host.DEEPSLATE, this.entries);
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
        Block block = BuiltInRegistries.BLOCK.get(entry.getBlockId());
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

    public List<OreRegenEntry> getAllEntries() {
        return this.entries;
    }

}
