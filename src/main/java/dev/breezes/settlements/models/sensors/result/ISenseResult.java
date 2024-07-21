package dev.breezes.settlements.models.sensors.result;

import dev.breezes.settlements.models.brain.IBrain;

import javax.annotation.Nonnull;
import java.util.List;

public interface ISenseResult {

    List<ISenseResultEntry<?>> getSenseResults();

    default void saveToMemory(@Nonnull IBrain brain) {
        this.getSenseResults()
                .forEach((senseResultEntry) -> senseResultEntry.saveToBrain(brain));
    }

}
