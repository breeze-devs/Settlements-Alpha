package dev.breezes.settlements.infrastructure.minecraft.persistence;

import dev.breezes.settlements.domain.settlement.model.SettlementMetadata;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SettlementSavedData extends SavedData {

    private static final String DATA_NAME = "settlements_settlement_metadata";
    private static final String ENTRIES_TAG = "entries";
    private static final String SETTLEMENT_ID_TAG = "settlement_id";
    private static final String NAME_TAG = "name";
    private static final String PRIMARY_TRAIT_TAG = "primary_trait";
    private static final String SCALE_TIER_TAG = "scale_tier";
    private static final String CENTER_X_TAG = "center_x";
    private static final String CENTER_Z_TAG = "center_z";
    private static final String BOUNDS_MIN_X_TAG = "bounds_min_x";
    private static final String BOUNDS_MIN_Z_TAG = "bounds_min_z";
    private static final String BOUNDS_MAX_X_TAG = "bounds_max_x";
    private static final String BOUNDS_MAX_Z_TAG = "bounds_max_z";
    private static final String ESTIMATED_POPULATION_TAG = "estimated_population";
    private static final String WEALTH_LEVEL_TAG = "wealth_level";

    private final Map<String, SettlementMetadata> metadataBySettlementId;

    public SettlementSavedData() {
        this(new HashMap<>());
    }

    private SettlementSavedData(@Nonnull Map<String, SettlementMetadata> metadataBySettlementId) {
        this.metadataBySettlementId = metadataBySettlementId;
    }

    public static SettlementSavedData get(@Nonnull MinecraftServer server) {
        return get(server.overworld());
    }

    public static SettlementSavedData get(@Nonnull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SettlementSavedData::new, SettlementSavedData::load),
                DATA_NAME
        );
    }

    public static SettlementSavedData load(@Nonnull CompoundTag tag,
                                           @Nonnull HolderLookup.Provider registries) {
        Map<String, SettlementMetadata> metadataBySettlementId = new HashMap<>();
        ListTag entries = tag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (Tag rawEntry : entries) {
            if (!(rawEntry instanceof CompoundTag entry)) {
                continue;
            }

            SettlementMetadata metadata = SettlementMetadata.builder()
                    .settlementId(entry.getString(SETTLEMENT_ID_TAG))
                    .name(entry.getString(NAME_TAG))
                    .primaryTrait(entry.getString(PRIMARY_TRAIT_TAG))
                    .scaleTier(entry.getString(SCALE_TIER_TAG))
                    .centerX(entry.getInt(CENTER_X_TAG))
                    .centerZ(entry.getInt(CENTER_Z_TAG))
                    .boundsMinX(entry.contains(BOUNDS_MIN_X_TAG) ? entry.getInt(BOUNDS_MIN_X_TAG) : Integer.MIN_VALUE)
                    .boundsMinZ(entry.contains(BOUNDS_MIN_Z_TAG) ? entry.getInt(BOUNDS_MIN_Z_TAG) : Integer.MIN_VALUE)
                    .boundsMaxX(entry.contains(BOUNDS_MAX_X_TAG) ? entry.getInt(BOUNDS_MAX_X_TAG) : Integer.MAX_VALUE)
                    .boundsMaxZ(entry.contains(BOUNDS_MAX_Z_TAG) ? entry.getInt(BOUNDS_MAX_Z_TAG) : Integer.MAX_VALUE)
                    .estimatedPopulation(entry.getInt(ESTIMATED_POPULATION_TAG))
                    .wealthLevel(entry.getFloat(WEALTH_LEVEL_TAG))
                    .build();
            metadataBySettlementId.put(metadata.settlementId(), metadata);
        }

        return new SettlementSavedData(metadataBySettlementId);
    }

    public void put(@Nonnull SettlementMetadata metadata) {
        SettlementMetadata previous = this.metadataBySettlementId.put(metadata.settlementId(), metadata);
        if (!metadata.equals(previous)) {
            this.setDirty();
        }
    }

    public Optional<SettlementMetadata> getBySettlementId(@Nonnull String settlementId) {
        return Optional.ofNullable(this.metadataBySettlementId.get(settlementId));
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag,
                            @Nonnull HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (SettlementMetadata metadata : this.metadataBySettlementId.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(SETTLEMENT_ID_TAG, metadata.settlementId());
            entry.putString(NAME_TAG, metadata.name());
            entry.putString(PRIMARY_TRAIT_TAG, metadata.primaryTrait());
            entry.putString(SCALE_TIER_TAG, metadata.scaleTier());
            entry.putInt(CENTER_X_TAG, metadata.centerX());
            entry.putInt(CENTER_Z_TAG, metadata.centerZ());
            entry.putInt(BOUNDS_MIN_X_TAG, metadata.boundsMinX());
            entry.putInt(BOUNDS_MIN_Z_TAG, metadata.boundsMinZ());
            entry.putInt(BOUNDS_MAX_X_TAG, metadata.boundsMaxX());
            entry.putInt(BOUNDS_MAX_Z_TAG, metadata.boundsMaxZ());
            entry.putInt(ESTIMATED_POPULATION_TAG, metadata.estimatedPopulation());
            entry.putFloat(WEALTH_LEVEL_TAG, metadata.wealthLevel());
            entries.add(entry);
        }
        tag.put(ENTRIES_TAG, entries);
        return tag;
    }

}
