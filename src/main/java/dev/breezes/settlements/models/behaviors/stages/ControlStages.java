package dev.breezes.settlements.models.behaviors.stages;

public class ControlStages {

    private static final int RANDOM_STRING_LENGTH = 5;

    public static Stage newStepStartStage() {
        return new SimpleStage("STEP_START");
    }

    /**
     * Signals the end of the step behavior. If nested, will fall through to the parent step's next step
     * <p>
     * Singleton stage across all behaviors
     */
    public static final Stage STEP_END = new SimpleStage("STEP_END");

}
