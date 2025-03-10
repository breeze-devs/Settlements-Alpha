package dev.breezes.settlements.models.behaviors.stages;

import lombok.AllArgsConstructor;
import lombok.CustomLog;

import java.util.Set;

/**
 *
 */
@AllArgsConstructor
@CustomLog
public class StageController {

    private final Set<Stage> validStages;

    private Stage currentStage;

    public void changeStage(Stage stage) {
        if (!this.validStages.contains(stage)) {
            // LOGTODO
            log.error("Invalid stage: " + stage);
            return;
        }

        this.currentStage = stage;
    }

}
