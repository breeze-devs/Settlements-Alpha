package dev.breezes.settlements.infrastructure.minecraft.data.schedule;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Client-side mirror of the village work rhythm's verdict, and the calendar day that verdict speaks for.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class VillageDayTypeClientProjection implements ClientSessionResettable {

    @Nullable
    private PlanDayType dayType;
    private long calendarDay;

    public void applySnapshot(long calendarDay, @Nonnull PlanDayType dayType) {
        this.calendarDay = calendarDay;
        this.dayType = dayType;
    }

    /**
     * The rhythm's verdict for the given calendar day, empty when the server has not yet spoken for that day.
     */
    public Optional<PlanDayType> dayTypeFor(long calendarDay) {
        if (this.dayType == null || this.calendarDay != calendarDay) {
            return Optional.empty();
        }
        return Optional.of(this.dayType);
    }

    @Override
    public void onClientSessionEnded() {
        this.dayType = null;
        this.calendarDay = 0L;
    }

}
