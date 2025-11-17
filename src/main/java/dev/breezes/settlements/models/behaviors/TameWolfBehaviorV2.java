package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.configurations.annotations.ConfigurationType;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapEntry;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.wolves.SettlementsWolf;
import dev.breezes.settlements.models.behaviors.stages.ControlStages;
import dev.breezes.settlements.models.behaviors.stages.SimpleStage;
import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetableType;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.TimeBasedStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.StayCloseStep;
import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.models.conditions.NearbyEntityExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.Expertise;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.util.RandomUtil;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@CustomLog
public class TameWolfBehaviorV2 extends BaseVillagerBehavior {

    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 10, min = 1)
    private static int preconditionCheckCooldownMin;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 20, min = 1)
    private static int preconditionCheckCooldownMax;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 60, min = 1)
    private static int behaviorCooldownMin;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 240, min = 1)
    private static int behaviorCooldownMax;

    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = "scan_range_horizontal",
            description = "Horizontal range (in blocks) to scan for nearby wolves to tame",
            defaultValue = 32, min = 5, max = 128)
    private static int scanRangeHorizontal;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = "scan_range_vertical",
            description = "Vertical range (in blocks) to scan for nearby wolves to tame",
            defaultValue = 12, min = 1, max = 16)
    private static int scanRangeVertical;

    private static final double TAME_SUCCESS_CHANCE = 0.33;
    private static final int MAX_TAME_ATTEMPTS = 5;


    @MapConfig(type = ConfigurationType.BEHAVIOR,
            identifier = "expertise_wolf_limit",
            description = "Map of expertise to the maximum number of wolves a villager can own",
            deserializer = "StringToInteger",
            defaultValue = {
                    @MapEntry(key = "novice", value = "1"),
                    @MapEntry(key = "apprentice", value = "1"),
                    @MapEntry(key = "journeyman", value = "2"),
                    @MapEntry(key = "expert", value = "2"),
                    @MapEntry(key = "master", value = "3")
            })
    private static Map<String, Integer> expertiseWolfLimit;

    private static final Stage TAME_WOLF = new SimpleStage("TAME_WOLF");

    private final StagedStep controlStep;

    private final NearbyEntityExistsCondition<BaseVillager, Wolf> nearbyUntamedWolfExistsCondition;

    private int attemptsRemaining;

    @Nullable
    private BehaviorContext context;

    public TameWolfBehaviorV2() {
        super(log,
                RandomRangeTickable.of(Ticks.of(preconditionCheckCooldownMin), Ticks.of(preconditionCheckCooldownMax)),
                RandomRangeTickable.of(Ticks.of(behaviorCooldownMin), Ticks.of(behaviorCooldownMax))
        );

        this.context = null;
        this.attemptsRemaining = 0;

        // Preconditions: at least one untamed wolf nearby
        this.nearbyUntamedWolfExistsCondition = new NearbyEntityExistsCondition<>(
                scanRangeHorizontal,
                scanRangeVertical,
                EntityType.WOLF,
                wolf -> wolf != null && !wolf.isTame(),
                1
        );
        this.preconditions.add(this.nearbyUntamedWolfExistsCondition);

        // Steps controller
        this.controlStep = StagedStep.builder()
                .name("TameWolfBehaviorV2")
                .initialStage(TAME_WOLF)
                .stageStepMap(Map.of(
                        TAME_WOLF, this.createTameWolfStep()
                ))
                .nextStage(ControlStages.STEP_END)
                .onEnd(ctx -> Optional.empty())
                .build();
    }

    private BehaviorStep createTameWolfStep() {
        TimeBasedStep attemptStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(1).asTickable())
                .onStart(ctx -> {
                    // Consume one attempt when we start the action window
                    this.attemptsRemaining = Math.max(0, this.attemptsRemaining - 1);
                    return Optional.empty();
                })
                .everyTick(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.BONE.getDefaultInstance());
                    return Optional.empty();
                })
                .addKeyFrame(Ticks.seconds(0.5), ctx -> {
                    Optional<Wolf> wolfOptional = this.getTargetWolf(ctx);
                    if (wolfOptional.isEmpty()) {
                        return Optional.of(ControlStages.STEP_END);
                    }
                    Wolf wolf = wolfOptional.get();

                    // If already tamed somehow, end
                    if (wolf.isTame()) {
                        return Optional.of(ControlStages.STEP_END);
                    }

                    // Try to tame
                    log.behaviorStatus("Attempting to tame wolf {}", wolf.getUUID());

                    if (RandomUtil.chance(TAME_SUCCESS_CHANCE)) {
                        // Success effects
                        Location wolfLoc = Location.fromEntity(wolf, false);
                        wolfLoc.displayParticles(ParticleTypes.HEART, 7, 0.4, 0.5, 0.4, 0.01);
                        Location villagerHead = Location.fromEntity(ctx.getInitiator().getMinecraftEntity(), true);
                        villagerHead.displayParticles(ParticleTypes.HAPPY_VILLAGER, 8, 0.3, 0.2, 0.3, 0.01);
                        wolfLoc.playSound(SoundEvents.WOLF_AMBIENT, 0.6f, 1.5f, SoundSource.NEUTRAL);
                        wolfLoc.playSound(SoundEvents.WOLF_AMBIENT, 0.6f, 1.7f, SoundSource.NEUTRAL);

                        // Replace vanilla wolf with modded wolf
                        wolf.discard();

                        SettlementsWolf settlementsWolf = SettlementsWolf.spawn(wolfLoc);
                        settlementsWolf.setTame(true, true);
                        settlementsWolf.setOwnerUUID(ctx.getInitiator().getMinecraftEntity().getUUID());
                        settlementsWolf.setCollarColor(DyeColor.LIME);

                        log.behaviorStatus("Successfully tamed wolf {}", settlementsWolf.getUUID());

                        // Stop the behavior after success
                        return Optional.of(ControlStages.STEP_END);
                    } else {
                        // Failure effects
                        Location wolfLoc = Location.fromEntity(wolf, false);
                        wolfLoc.displayParticles(ParticleTypes.SMOKE, 6, 0.35, 0.5, 0.35, 0.01);
                        log.behaviorStatus("Failed to tame wolf");
                    }

                    log.behaviorStatus("Failed to tame wolf");
                    return Optional.empty();
                })
                .onEnd(ctx -> {
                    // Clear held item
                    ctx.getInitiator().clearHeldItem();

                    // Retry only against the same locked target
                    if (this.attemptsRemaining > 0) {
                        Optional<Wolf> target = this.getTargetWolf(ctx);
                        if (target.isPresent() && target.get().isAlive() && !target.get().isTame()) {
                            return Optional.of(TAME_WOLF);
                        }
                    }

                    return Optional.of(ControlStages.STEP_END);
                })
                .build();

        return StayCloseStep.builder()
                .closeEnoughDistance(2.5)
                .navigateStep(new NavigateToTargetStep(0.55f, 2))
                .actionStep(attemptStep)
                .build();
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.context = new BehaviorContext(entity);

        // Enforce wolf ownership limit per expertise
        Expertise expertise = entity.getExpertise();
        int limit = expertiseWolfLimit.getOrDefault(expertise.getConfigName(), 1);

        // TODO: this logic should be replaced by memory-based systems and a direct level entity get if alive
        int owned = entity.level().getEntitiesOfClass(Wolf.class,
                        entity.getBoundingBox().inflate(48, 16, 48),
                        wolf -> wolf.isTame() && ownerMatches(entity, wolf))
                .size();
        if (owned >= limit) {
            log.behaviorStatus("Owned wolves {} >= limit {}, aborting tame", owned, limit);
            this.requestStop();
            return;
        }

        if (!this.nearbyUntamedWolfExistsCondition.test(entity)) {
            this.requestStop();
            return;
        }

        Optional<Wolf> chosenWolf = this.nearbyUntamedWolfExistsCondition.getTargets().stream().findFirst();
        if (chosenWolf.isEmpty()) {
            this.requestStop();
            return;
        }
        this.context.setState(BehaviorStateType.TARGET, TargetState.of(List.of(Targetable.fromEntity(chosenWolf.get()))));
        this.attemptsRemaining = MAX_TAME_ATTEMPTS;
    }

    @Override
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        if (this.controlStep.getCurrentStage() == ControlStages.STEP_END) {
            throw new StopBehaviorException("Behavior has ended");
        }
        if (this.context == null) {
            throw new StopBehaviorException("Behavior context is null");
        }
        this.controlStep.tick(this.context);
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        entity.clearHeldItem();
        this.context = null;
        this.controlStep.reset();
    }

    private Optional<Wolf> getTargetWolf(@Nonnull BehaviorContext context) {
        ICondition<Targetable> isWolfPredicate = (target) -> target != null
                && target.getType() == TargetableType.ENTITY
                && target.getAsEntity().getType() == EntityType.WOLF;

        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .map(targetState -> targetState.match(isWolfPredicate))
                .flatMap(Stream::findFirst)
                .map(Targetable::getAsEntity)
                .map(Wolf.class::cast);
    }

    private static boolean ownerMatches(@Nonnull BaseVillager villager, @Nonnull Wolf wolf) {
        try {
            return villager.getUUID().equals(wolf.getOwnerUUID());
        } catch (Throwable t) {
            // Fallback using getOwner, if available
            return wolf.getOwner() != null && wolf.getOwner().getUUID().equals(villager.getUUID());
        }
    }
}
