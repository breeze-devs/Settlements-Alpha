package dev.breezes.settlements.application.ai.inference.plan;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

/**
 * Configuration record for the LLM PLAN overlay.
 * <p>
 * {@link PlanInferenceMode#HEURISTIC} is the default — HeuristicPlanGenerator remains the sole
 * source of day plans and no PLAN request is ever sent, even if an inference endpoint is
 * configured. The master kill-switch is still {@code InferenceConfig.endpointBaseUrl == ""};
 * this config only adds the mode gate and the overlay's own per-batch deadline on top of that.
 */
@BehaviorConfig(name = "plan_inference", type = ConfigurationType.GENERAL)
public record PlanInferenceConfig(

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "mode",
                description = "Plan mode: HEURISTIC (default) or LLM (an always-on per-villager overlay may replace the heuristic plan).",
                defaultValue = "HEURISTIC")
        String mode,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "overlay_deadline_seconds",
                description = "Total time budget in seconds for one PLAN overlay sub-batch's streaming request.",
                defaultValue = 300,
                min = 5,
                max = 1200)
        int overlayDeadlineSeconds,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "sub_batch_size",
                description = "Max villagers coalesced into one streaming PLAN POST. Purely bounds HTTP payload size and per-connection failure blast radius — the backend (SIS + vLLM continuous batching) owns actual LLM concurrency, so this is not a throughput knob.",
                defaultValue = 20,
                min = 1,
                max = 200)
        int subBatchSize,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "sub_batch_window_seconds",
                description = "Max real seconds a partial sub-batch is held before being sent. A sub-batch flushes when it reaches sub_batch_size villagers OR this window elapses since its first buffered villager, whichever comes first.",
                defaultValue = 3,
                min = 1,
                max = 30)
        int subBatchWindowSeconds,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "overlay_cutoff_civil_tick",
                description = "Latest point in a plan's own day (civil ticks; 0 = 00:00, 24000 = next midnight; default 11_000 = 11:00) at which an LLM PLAN overlay is still worth requesting.",
                defaultValue = 11_000,
                min = 0,
                max = 24_000)
        int overlayCutoffCivilTick

) {

    /**
     * Resolves the {@code mode} string to a {@link PlanInferenceMode} enum value.
     * Unrecognized strings default to {@link PlanInferenceMode#HEURISTIC} — the LLM overlay
     * fails safe to the heuristic floor rather than fail-loud on a config typo.
     */
    public PlanInferenceMode resolvedMode() {
        try {
            return PlanInferenceMode.valueOf(this.mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PlanInferenceMode.HEURISTIC;
        }
    }

}
