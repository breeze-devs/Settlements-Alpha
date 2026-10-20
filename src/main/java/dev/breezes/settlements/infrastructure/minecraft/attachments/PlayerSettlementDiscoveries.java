package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.shared.codec.ListCodecs;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The settlements a player has discovered.
 * Immutable: each change returns a new value.
 *
 * @param entries the discovered settlements, in the order they were discovered
 */
public record PlayerSettlementDiscoveries(Set<Entry> entries) {

    /**
     * A player who has discovered no settlement.
     */
    public static final PlayerSettlementDiscoveries NONE = new PlayerSettlementDiscoveries(Set.of());

    /**
     * The saved form. An entry that cannot be read is dropped on its own, keeping the others.
     */
    public static final Codec<PlayerSettlementDiscoveries> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ListCodecs.lenient(Entry.CODEC, "settlement discovery").fieldOf("settlements")
                    .forGetter(discoveries -> List.copyOf(discoveries.entries()))
    ).apply(instance, entries -> new PlayerSettlementDiscoveries(new LinkedHashSet<>(entries))));

    public PlayerSettlementDiscoveries {
        entries = Collections.unmodifiableSet(new LinkedHashSet<>(entries));
    }

    /**
     * Returns whether the player has discovered the dimension's settlement with the given id.
     */
    public boolean contains(@Nonnull ResourceKey<Level> dimension, @Nonnull UUID settlementId) {
        return this.entries.contains(new Entry(dimension, settlementId));
    }

    /**
     * Returns these discoveries with the dimension's settlement with the given id among them.
     */
    public PlayerSettlementDiscoveries with(@Nonnull ResourceKey<Level> dimension, @Nonnull UUID settlementId) {
        Set<Entry> grown = new LinkedHashSet<>(this.entries);
        grown.add(new Entry(dimension, settlementId));
        return new PlayerSettlementDiscoveries(grown);
    }

    /**
     * Returns these discoveries without the settlements that a loaded dimension no longer stores. An entry whose
     * dimension is not loaded is kept, since nothing can tell whether its settlement still stands.
     *
     * @param isLoaded whether the server has a level for the dimension
     * @param isStored whether the dimension's level stores the settlement with the given id; asked only about loaded
     *                 dimensions
     */
    public PlayerSettlementDiscoveries pruned(@Nonnull Predicate<ResourceKey<Level>> isLoaded,
                                              @Nonnull BiPredicate<ResourceKey<Level>, UUID> isStored) {
        return new PlayerSettlementDiscoveries(this.entries.stream()
                .filter(entry -> !isLoaded.test(entry.dimension())
                        || isStored.test(entry.dimension(), entry.settlementId()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * One discovered settlement. Its id names it only within its dimension, since generated settlements in different
     * dimensions can share an id.
     */
    public record Entry(ResourceKey<Level> dimension, UUID settlementId) {

        /**
         * Both fields are required: an entry missing either is unreadable, never read with a guessed value.
         */
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Entry::dimension),
                UUIDUtil.CODEC.fieldOf("settlement_id").forGetter(Entry::settlementId)
        ).apply(instance, Entry::new));

    }

}
