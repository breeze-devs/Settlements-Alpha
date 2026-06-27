package dev.breezes.settlements.application.ai.brain;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryAccess;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.ObservationReport;
import dev.breezes.settlements.domain.ai.memory.SettlementsMemoryStore;
import dev.breezes.settlements.domain.ai.sensors.ISensor;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@CustomLog
public class VillagerBrain implements IBrain {

    private final BaseVillager villager;
    private final MemoryAccess access;

    private List<ISensor<BaseVillager>> sensors;

    public VillagerBrain(@Nonnull BaseVillager villager) {
        this.villager = villager;
        this.access = new VillagerMemoryAccess(villager);
        this.sensors = List.of();
    }

    /**
     * Builds this villager's sensors from the server graph. Deferred out of the constructor because the
     * server component is unavailable during entity construction and on the client; the villager calls
     * this from its server-side spawn/load hooks, before the first tick.
     */
    @Override
    public void initialize() {
        this.sensors = SettlementsDagger.serverOrThrow()
                .villagerSensorFactories()
                .stream()
                .map(factory -> factory.create(this.villager))
                .toList();
    }

    @Override
    public void tick(int delta) {
        Level level = this.villager.level();
        for (ISensor<BaseVillager> sensor : this.sensors) {
            sensor.tick(delta, level, this.villager);
        }
    }

    @Override
    public void forceSensorScan(@Nonnull Level world) {
        // Run each sensor's doSense unconditionally, bypassing internal cooldown timers.
        // Writes are applied to the brain's memory immediately so the postcondition of the
        // Investigate behavior (memories updated) is deterministic on the same tick.
        for (ISensor<BaseVillager> sensor : this.sensors) {
            sensor.doSense(world, this.villager)
                    .forEach(write -> write.applyTo(this));
        }
    }

    @Override
    public <T> Optional<T> getMemory(@Nonnull MemoryType<T> type) {
        // No isDecaying() branch — the type itself knows where to read from.
        return type.read(this.access);
    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value) {
        // DecayingSpatialMemoryType.write throws UnsupportedOperationException — correct by design.
        type.write(this.access, value);
    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value, @Nonnull ClockTicks expiration) {
        type.write(this.access, value, expiration.getTicks());
    }

    @Override
    public void clearMemory(@Nonnull MemoryType<?> type) {
        // No isDecaying() branch, no cast — each impl clears from its own backing.
        type.clear(this.access);
    }

    @Override
    public void updateObservation(@Nonnull MemoryType.DecayingSpatialMemoryType type,
                                  @Nonnull ObservationReport report,
                                  long nowTick) {
        this.access.decayingStore().updateSpatialObservation(type, report, nowTick);
    }

    /**
     * Adapts {@link BaseVillager} to the {@link MemoryAccess} port.
     * Vanilla operations delegate to {@code villager.getBrain()}; decaying operations
     * read the {@code SETTLEMENTS_MEMORY_STORE} attachment. Dimension and tick are
     * computed on demand so the adapter can be constructed once per brain.
     */
    private static final class VillagerMemoryAccess implements MemoryAccess {

        private final BaseVillager villager;

        private VillagerMemoryAccess(BaseVillager villager) {
            this.villager = villager;
        }

        @Override
        public <T> Optional<T> vanillaGet(MemoryModuleType<T> module) {
            return this.villager.getBrain().getMemory(module);
        }

        @Override
        public <T> void vanillaSet(MemoryModuleType<T> module, T value) {
            this.villager.getBrain().setMemory(module, value);
        }

        @Override
        public <T> void vanillaSet(MemoryModuleType<T> module, T value, long expirationTicks) {
            this.villager.getBrain().setMemoryWithExpiry(module, value, expirationTicks);
        }

        @Override
        public void vanillaErase(MemoryModuleType<?> module) {
            this.villager.getBrain().eraseMemory(module);
        }

        @Override
        public SettlementsMemoryStore decayingStore() {
            // The attachment is transient (no serialization), so the store is always live
            // during the server session.
            return this.villager.getData(AttachmentRegistry.SETTLEMENTS_MEMORY_STORE);
        }

        @Override
        @Nullable
        public ResourceKey<Level> serverDimensionOrNull() {
            Level level = this.villager.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return null;
            }

            return serverLevel.dimension();
        }

        @Override
        public long nowTick() {
            return this.villager.level().getGameTime();
        }

    }

}
