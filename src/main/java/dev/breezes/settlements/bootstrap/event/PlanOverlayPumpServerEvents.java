package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ai.inference.plan.PlanInferenceConfig;
import dev.breezes.settlements.application.ai.inference.plan.PlanInferenceMode;
import dev.breezes.settlements.application.ai.planning.PlanRequestService;
import dev.breezes.settlements.di.ServerScope;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Pumps the LLM PLAN overlay's chunked context assembly every server tick.
 * <p>
 * The overlay itself is always-on and per-villager — {@code PlanRunner#submitNextPlanAsync} stages
 * each villager individually via {@code PlanRequestService#enqueueForOverlay} as its plan is
 * exhausted, rather than this class firing a periodic sweep. This class's only remaining job is to
 * drain whatever is staged, a few villagers per tick, so context assembly never blocks the server
 * thread for a whole village at once.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PlanOverlayPumpServerEvents {

    private final PlanRequestService planRequestService;
    private final PlanInferenceConfig config;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (this.config.resolvedMode() == PlanInferenceMode.HEURISTIC) {
            return;
        }

        // Use wall clock, not game time. This should not be influenced by server TPS
        this.planRequestService.pumpAssembly(System.nanoTime());
    }

}
