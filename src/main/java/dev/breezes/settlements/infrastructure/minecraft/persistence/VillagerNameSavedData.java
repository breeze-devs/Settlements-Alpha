package dev.breezes.settlements.infrastructure.minecraft.persistence;

import dev.breezes.settlements.domain.ai.naming.VillagerNameTable;
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
import java.util.UUID;

/**
 * Thin serialization shell around {@link VillagerNameTable}.
 * <p>
 * All map logic lives on the table; this class only knows how to turn it into NBT and
 * back, and to forward {@code setDirty()} to the engine when the table reports that a
 * call actually changed its content.
 */
public final class VillagerNameSavedData extends SavedData {

    private static final String DATA_NAME = "settlements_villager_names";
    private static final String ENTRIES_TAG = "entries";
    private static final String UUID_TAG = "uuid";
    private static final String NAME_TAG = "name";

    private final VillagerNameTable table;

    public VillagerNameSavedData() {
        this(new VillagerNameTable());
    }

    private VillagerNameSavedData(@Nonnull VillagerNameTable table) {
        this.table = table;
    }

    public static VillagerNameSavedData get(@Nonnull MinecraftServer server) {
        return get(server.overworld());
    }

    public static VillagerNameSavedData get(@Nonnull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillagerNameSavedData::new, VillagerNameSavedData::load),
                DATA_NAME
        );
    }

    public static VillagerNameSavedData load(@Nonnull CompoundTag tag,
                                             @Nonnull HolderLookup.Provider registries) {
        Map<UUID, String> namesByUuid = new HashMap<>();
        ListTag entries = tag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);

        for (Tag rawEntry : entries) {
            // A malformed entry is skipped -- the villager re-registers the next time it loads
            if (!(rawEntry instanceof CompoundTag entry) || !entry.hasUUID(UUID_TAG)) {
                continue;
            }

            String name = entry.getString(NAME_TAG);
            if (name.isBlank()) {
                continue;
            }

            namesByUuid.put(entry.getUUID(UUID_TAG), name);
        }

        return new VillagerNameSavedData(new VillagerNameTable(namesByUuid));
    }

    public String resolve(@Nonnull UUID uuid) {
        return this.table.resolve(uuid);
    }

    public void upsert(@Nonnull UUID uuid, @Nonnull String name) {
        this.table.upsert(uuid, name);

        if (this.table.isDirty()) {
            this.setDirty();
            this.table.clearDirty();
        }
    }

    public void remove(@Nonnull UUID uuid) {
        this.table.remove(uuid);

        if (this.table.isDirty()) {
            this.setDirty();
            this.table.clearDirty();
        }
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag,
                            @Nonnull HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, String> entry : this.table.entriesView().entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(UUID_TAG, entry.getKey());
            entryTag.putString(NAME_TAG, entry.getValue());
            entries.add(entryTag);
        }

        tag.put(ENTRIES_TAG, entries);
        return tag;
    }

}
