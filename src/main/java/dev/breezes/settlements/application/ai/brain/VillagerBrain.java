package dev.breezes.settlements.application.ai.brain;

import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.sensors.ISensor;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

@CustomLog
public class VillagerBrain implements IBrain {

    private final BaseVillager villager;

    private List<ISensor<BaseVillager>> sensors;

    public VillagerBrain(@Nonnull BaseVillager villager) {
        this.villager = villager;
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
    public <T> Optional<T> getMemory(@Nonnull MemoryType<T> type) {
        return this.villager.getBrain().getMemory(type.getModuleType());
    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value) {
        this.villager.getBrain().setMemory(type.getModuleType(), value);
    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value, @Nonnull ClockTicks expiration) {
        this.villager.getBrain().setMemoryWithExpiry(type.getModuleType(), value, expiration.getTicks());
    }

    @Override
    public void clearMemory(@Nonnull MemoryType<?> type) {
        this.villager.getBrain().eraseMemory(type.getModuleType());
    }

}
