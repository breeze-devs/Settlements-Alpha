package dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The cartographer sketches a map and detects enemies in the active raid.
 */
@CustomLog(topic = "behavior")
public class ChartRaidersBehavior extends VillagerStateMachineBehavior {

    /**
     * The wait after a scan that charted nobody.
     * Keeps a cartographer from sketching back to back while other cartographers have every raider lit.
     */
    private static final ClockTicks EMPTY_PULSE_RETRY = ClockTicks.seconds(15);

    private static final int DEFAULT_RAIDERS_PER_PULSE = 2;

    private enum ChartStage implements StageKey {
        SKETCH,
        END;
    }

    private final ChartRaidersConfig config;

    // Runtime state
    private boolean shouldRewardExperience;

    public ChartRaidersBehavior(@Nonnull ChartRaidersConfig config,
                                @Nonnull BehaviorSupport support) {
        // The cadence is held in a brain memory, so the behavior's own cooldowns stay empty
        super(log, ClockTicks.ZERO.asTickable(), ClockTicks.ZERO.asTickable(), support, config.experienceReward());
        this.config = config;
        this.shouldRewardExperience = false;
        this.initializeStateMachine(StagedStep.<BaseVillager>builder()
                .name("ChartRaidersBehavior")
                .initialStage(ChartStage.SKETCH)
                .stageStepMap(Map.of(ChartStage.SKETCH, MapSketch.step(this::chartRaiders)))
                .nextStage(ChartStage.END)
                .build(), ChartStage.END);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.shouldRewardExperience = false;
        // Launched as the sketch begins, so the rocket is in the air for the whole sketch
        ChartFlares.launchSignal(context.getLevel(), villager, villager.getExpertise(), villager.getRandom());
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
    }

    private StepResult chartRaiders(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager villager = context.getInitiator();
        ServerLevel level = context.getLevel();

        // The wave can end during the sketch. No cooldown then, so the next wave is charted as soon as it spawns
        Optional<Raid> raid = RaidCharting.raidInWave(level, villager);
        if (raid.isEmpty()) {
            return StepResult.transition(ChartStage.END);
        }

        int raidersPerPulse = this.config.expertiseRaidersPerPulse()
                .getOrDefault(villager.getExpertise().getConfigName(), DEFAULT_RAIDERS_PER_PULSE);
        List<Raider> candidates = raid.get().getAllRaiders().stream()
                .filter(raider -> RaidCharting.isChartable(raider, level))
                .limit(raidersPerPulse)
                .toList();

        List<Raider> charted = new ArrayList<>();
        for (Raider raider : candidates) {
            if (raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, RaidCharting.GLOW_DURATION.getTicksAsInt()), villager)) {
                charted.add(raider);
            }
        }

        ClockTicks cadence = this.config.cadence();
        if (charted.isEmpty()) {
            ClockTicks retry = ClockTicks.of(Math.min(EMPTY_PULSE_RETRY.getTicks(), cadence.getTicks()));
            villager.getSettlementsBrain().setMemory(MemoryTypeRegistry.CHART_RAIDERS_COOLDOWN, true, retry);
            return StepResult.transition(ChartStage.END);
        }

        villager.getSettlementsBrain().setMemory(MemoryTypeRegistry.CHART_RAIDERS_COOLDOWN, true, cadence);
        Location.fromEntity(villager, false).playSound(SoundEvents.BELL_RESONATE, 1.0F, 1.0F, SoundSource.NEUTRAL);
        ChartFlares.send(level, raid.get(), charted, villager.getExpertise(), villager.getRandom());

        this.shouldRewardExperience = true;
        context.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.RAIDERS_CHARTED, "raiders"))
                .recordYield(charted.size());
        return StepResult.transition(ChartStage.END);
    }

}
