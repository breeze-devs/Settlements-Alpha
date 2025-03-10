package dev.breezes.settlements.models.behaviors.steps.gg;

import dev.breezes.settlements.models.behaviors.stages.ControlStages;
import dev.breezes.settlements.models.behaviors.stages.SimpleStage;
import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.WaitStep;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CustomLog
public class BehaviorSequence {

    private final BehaviorContext context;
    private final List<BehaviorStep> steps;

    private BehaviorSequence(@Nonnull BehaviorContext context) {
        this.context = context;
        this.steps = new ArrayList<>();
    }

    public static BehaviorSequence of(@Nonnull BehaviorContext context) {
        return new BehaviorSequence(context);
    }

    public BehaviorSequence addStep(@Nonnull BehaviorStep step) {
        this.steps.add(step);
        return this;
    }

//    public void execute() {
//        Object currentResponse = null;
//        for (BehaviorStep<?> step : this.steps) {
//            // noinspection unchecked
//            BehaviorStep<Object> stepCasted = (BehaviorStep<Object>) step;
//            currentResponse = stepCasted.tick(this.context);
//        }
//        return (O) currentResponse;
//    }

    public static void main(String[] args) {
        // Z-level
        Stage subZAStage = new SimpleStage("SubZA");
        Stage subZBStage = new SimpleStage("SubZB");

        // A-level
        Stage subA1Stage = new SimpleStage("SubA1");
        Stage subA2Stage = new SimpleStage("SubA2");
        WaitStep subA1 = WaitStep.builder()
                .waitTime(Tickable.of(Ticks.seconds(1)))
                .nextStage(subA2Stage)
                .build();
        WaitStep subA2 = WaitStep.builder()
                .waitTime(Tickable.of(Ticks.seconds(1)))
                .nextStage(ControlStages.STEP_END)
                .build();
        Map<Stage, BehaviorStep> stageStepMapA = Map.of(
                subA1Stage, subA1,
                subA2Stage, subA2
        );
        StagedStep stepA = StagedStep.builder()
                .name("A")
                .stageStepMap(stageStepMapA)
                .initialStage(subA1Stage)
                .nextStage(subZBStage)
                .build();

        // B-level
        Stage subB1Stage = new SimpleStage("SubB1");
        Stage subB2Stage = new SimpleStage("SubB2");
        WaitStep subB1 = WaitStep.builder()
                .waitTime(Tickable.of(Ticks.seconds(1)))
                .nextStage(subB2Stage)
                .build();
        WaitStep subB2 = WaitStep.builder()
                .waitTime(Tickable.of(Ticks.seconds(1)))
                .nextStage(ControlStages.STEP_END)
                .build();
        Map<Stage, BehaviorStep> stageStepMapB = Map.of(
                subB1Stage, subB1,
                subB2Stage, subB2
        );
        StagedStep stepB = StagedStep.builder()
                .name("B")
                .stageStepMap(stageStepMapB)
                .initialStage(subB1Stage)
                .nextStage(ControlStages.STEP_END)
                .build();

        // Z-level
        Map<Stage, BehaviorStep> stageStepMapZ = Map.of(
                subZAStage, stepA,
                subZBStage, stepB
        );
        StagedStep stepZ = StagedStep.builder()
                .name("Z")
                .stageStepMap(stageStepMapZ)
                .initialStage(subZAStage)
                .nextStage(ControlStages.STEP_END)
                .build();

        BehaviorContext context = new BehaviorContext(null);

        log.info("Starting test staged step");
        while (stepZ.getCurrentStage() != ControlStages.STEP_END) {
            stepZ.tick(context);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        log.info("Ending test staged step");
    }


}
