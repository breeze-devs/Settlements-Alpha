package dev.breezes.settlements.domain.ai.naming;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory {@link VillagerNameDirectory}.
 */
public final class VillagerNameTable implements VillagerNameDirectory {

    private final Map<UUID, String> namesByUuid;
    @Getter
    private boolean dirty;

    public VillagerNameTable() {
        this(new HashMap<>());
    }

    public VillagerNameTable(@Nonnull Map<UUID, String> initialEntries) {
        this.namesByUuid = new HashMap<>(initialEntries);
    }

    @Override
    public String resolve(@Nonnull UUID uuid) {
        String stored = this.namesByUuid.get(uuid);
        return stored != null ? stored : VillagerNameGenerator.generateName(uuid);
    }

    @Override
    public void upsert(@Nonnull UUID uuid, @Nonnull String name) {
        String previous = this.namesByUuid.put(uuid, name);
        if (!name.equals(previous)) {
            this.dirty = true;
        }
    }

    @Override
    public void remove(@Nonnull UUID uuid) {
        String previous = this.namesByUuid.remove(uuid);
        if (previous != null) {
            this.dirty = true;
        }
    }

    public void clearDirty() {
        this.dirty = false;
    }

    /**
     * Snapshot of all entries, for the persistence shell's serialization step.
     */
    public Map<UUID, String> entriesView() {
        return Map.copyOf(this.namesByUuid);
    }

}
