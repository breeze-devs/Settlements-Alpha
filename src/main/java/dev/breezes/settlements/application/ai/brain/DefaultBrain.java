package dev.breezes.settlements.application.ai.brain;

import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.ObservationReport;
import dev.breezes.settlements.domain.ai.schedule.IScheduleProvider;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.Builder;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Optional;

public class DefaultBrain implements IBrain {

    // @Inject
    protected final IScheduleProvider scheduleProvider;

    @Builder
    private DefaultBrain() {
        this.scheduleProvider = null; // TODO: placeholder
    }

    @Override
    public void initialize() {
        // No server-scoped state to wire up
    }

    @Override
    public void tick(int delta) {

    }

    @Override
    public void forceSensorScan(@Nonnull Level world) {
        // Placeholder: DefaultBrain has no sensors to scan
    }

    @Override
    public <T> Optional<T> getMemory(@Nonnull MemoryType<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value) {

    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value, @Nonnull ClockTicks expiration) {

    }

    @Override
    public void clearMemory(@Nonnull MemoryType<?> type) {

    }

    @Override
    public void updateObservation(@Nonnull MemoryType.DecayingSpatialMemoryType type,
                                  @Nonnull ObservationReport report,
                                  long nowTick) {
        // DefaultBrain is a placeholder; observation updates are no-ops here.
    }

}
