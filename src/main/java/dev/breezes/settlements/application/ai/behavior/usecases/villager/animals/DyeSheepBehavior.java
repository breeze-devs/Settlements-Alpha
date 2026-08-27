package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetQueries;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.conditions.PerceivedEntityExistsCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.tags.EntityTag;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@CustomLog
public class DyeSheepBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    // Fallback when a villager's expertise level is missing from the configured limit map.
    private static final int DEFAULT_DYE_LIMIT = 1;

    private static final double WHITE_WEIGHT = 50.0;
    private static final double BLACK_WEIGHT = 10.0;
    private static final double GRAY_WEIGHT = 10.0;
    // Remaining colors share the remaining slice; white/black/gray dominate to mimic natural flocks
    private static final double OTHER_COLOR_WEIGHT = 30.0 / 13.0;

    // Dyeing a naturally-red sheep is a free "wololo" easter egg that always recolors it blue.
    private static final DyeColor WOLOLO_TRIGGER_COLOR = DyeColor.RED;
    private static final DyeColor WOLOLO_RESULT_COLOR = DyeColor.BLUE;

    private enum DyeStage implements StageKey {
        DYE_SHEEP,
        END;
    }

    private final DyeSheepConfig config;
    private final AtomicInteger dyeCount;
    private final Set<UUID> dyedSheepIds;
    @Nullable
    private DyeColor selectedDyeColor;
    private boolean shouldRewardExperience;

    public DyeSheepBehavior(@Nonnull DyeSheepConfig config,
                            @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support,
                config.experienceReward());

        this.config = config;
        this.dyeCount = new AtomicInteger(0);
        this.dyedSheepIds = new HashSet<>();
        this.selectedDyeColor = null;
        this.shouldRewardExperience = false;

        this.preconditions.add(PerceivedEntityExistsCondition.<BaseVillager, Sheep>builder()
                .entityType(Sheep.class)
                .filter(this::isEligibleSheep)
                .completionRange(1)
                .build());
        // Gate on carrying dye and signal demand so chest/trade procurement keeps shepherds stocked.
        this.preconditions.add(support.getDemandSignalService().requireItem(new ItemMatch.TagRef(Tags.Items.DYES), 1, 50,
                this.getClass().getSimpleName()));

        this.initializeStateMachine(this.createControlStep(), DyeStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("DyeSheepBehavior")
                .initialStage(DyeStage.DYE_SHEEP)
                .stageStepMap(Map.of(DyeStage.DYE_SHEEP, this.createDyeSheepStep()))
                .nextStage(DyeStage.END)
                .onEnd(context -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createDyeSheepStep() {
        TimeBasedStep<BaseVillager> dyeStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(1).asTickable())
                .onStart(context -> {
                    this.dyeCount.decrementAndGet();
                    context.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    this.setHeldDyePreview(context);

                    this.getTargetSheep(context).ifPresent(sheep -> {
                        if (isWololoTrigger(sheep)) {
                            Location location = Location.fromEntity(context.getInitiator(), true);
                            SoundRegistry.WOLOLO.playGlobally(location, SoundSource.NEUTRAL);
                        }
                    });

                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS), context -> {
                    Optional<Sheep> sheepOptional = this.getTargetSheep(context);
                    if (sheepOptional.isEmpty()) {
                        return StepResult.complete();
                    }

                    if (this.dyeSheep(context.getInitiator().getMinecraftEntity(), sheepOptional.get())) {
                        context.primaryDeed()
                                .ifPresent(outcome -> outcome.recordYield(1));
                        this.shouldRewardExperience = true;
                    }
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    if (this.dyeCount.get() > 0) {
                        List<Targetable> nearbySheep = this.findEligibleSheepTargets(context.getInitiator().getMinecraftEntity());
                        if (!nearbySheep.isEmpty()) {
                            context.setState(BehaviorStateType.TARGET, TargetState.of(nearbySheep));
                            return StepResult.transition(DyeStage.DYE_SHEEP);
                        }
                    }

                    return StepResult.complete();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(dyeStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {

        Expertise expertise = context.getInitiator().getMinecraftEntity().getExpertise();
        int limit = config.expertiseDyeLimit().getOrDefault(expertise.getConfigName(), DEFAULT_DYE_LIMIT);
        this.dyeCount.set(limit);
        this.dyedSheepIds.clear();
        this.selectedDyeColor = this.chooseBehaviorDyeColor(villager).orElse(null);
        this.shouldRewardExperience = false;
        log.behaviorStatus("Villager is '{}' level, maximum dye count is {}", expertise.toString(), limit);

        List<Targetable> targets = this.findEligibleSheepTargets(villager);
        if (targets.isEmpty()) {
            this.requestStop("No eligible sheep available");
            return;
        }
        context.setState(BehaviorStateType.TARGET, TargetState.of(targets));

        // Headline the deed with the run's color (wololo falls back to blue) so the deed reads "dyed N sheep blue".
        DyeColor headlineColor = this.selectedDyeColor != null ? this.selectedDyeColor : WOLOLO_RESULT_COLOR;
        context.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.SHEEP_DYED, "sheep", describeColor(headlineColor)));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        this.dyedSheepIds.clear();
        this.selectedDyeColor = null;
        this.shouldRewardExperience = false;
    }

    private void setHeldDyePreview(@Nonnull BehaviorContext<BaseVillager> context) {
        this.getTargetSheep(context)
                .flatMap(sheep -> this.resolveDyeColor(context.getInitiator().getMinecraftEntity(), sheep))
                .map(DyeItem::byColor)
                .map(Item::getDefaultInstance)
                .ifPresent(context.getInitiator()::setHeldItem);
    }

    private boolean dyeSheep(@Nonnull BaseVillager villager, @Nonnull Sheep sheep) {
        Optional<DyeColor> colorOptional = this.resolveDyeColor(villager, sheep);
        if (colorOptional.isEmpty()) {
            return false;
        }

        DyeColor color = colorOptional.get();
        // Wololo is an easter egg, so it stays free even when normal dyeing requires inventory.
        if (!isWololoTrigger(sheep) && !villager.getSettlementsInventory()
                .consumeIfRequired(DyeItem.byColor(color), 1, GeneralConfig.bypassInventoryRequirements)) {
            return false;
        }

        sheep.setColor(color);
        this.emitDyeEffects(sheep, color);
        this.dyedSheepIds.add(sheep.getUUID());
        return true;
    }

    private void emitDyeEffects(@Nonnull Sheep sheep, @Nonnull DyeColor color) {
        Location particleLocation = Location.fromEntity(sheep, false).add(0, 0.8, 0, true);
        ColorParticleOption dyeParticle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color.getTextureDiffuseColor());
        particleLocation.displayParticles(dyeParticle, 8, 0.4, 0.4, 0.4, 0.0);
        particleLocation.playSound(SoundEvents.ITEM_PICKUP, 0.4f, 0.9f, SoundSource.NEUTRAL);
    }

    private Optional<Sheep> getTargetSheep(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        return TargetQueries.firstEntity(context, EntityType.SHEEP, Sheep.class)
                .filter(sheep -> this.isEligibleSheep(villager, sheep));
    }

    private List<Targetable> findEligibleSheepTargets(@Nonnull BaseVillager villager) {
        return this.findEligibleSheep(villager).stream()
                .map(Targetable::fromEntity)
                .toList();
    }

    private List<Sheep> findEligibleSheep(@Nonnull BaseVillager villager) {
        return this.getPerceivedEntities(villager)
                .ofType(Sheep.class, sheep -> this.isEligibleSheep(villager, sheep)
                        && villager.getNavigationManager().canReach(Location.fromEntity(sheep, false), 1))
                .toList();
    }

    private boolean isEligibleSheep(@Nonnull BaseVillager villager, @Nonnull Sheep sheep) {
        return this.isUndyedVillageSheep(sheep)
                && !sheep.isSheared()
                && (isWololoTrigger(sheep) || this.canDyeWithBehaviorColor(villager, sheep));
    }

    /**
     * Shared base for every eligibility check: an animal the village owns (so the player's own flock is
     * left untouched) that this run has not already recolored.
     */
    private boolean isUndyedVillageSheep(@Nonnull Sheep sheep) {
        return isVillageOwned(sheep) && !this.dyedSheepIds.contains(sheep.getUUID());
    }

    private boolean canDyeWithBehaviorColor(@Nonnull BaseVillager villager, @Nonnull Sheep sheep) {
        if (this.selectedDyeColor != null) {
            return this.resolveDyeColor(villager, sheep).isPresent();
        }

        if (this.status != BehaviorStatus.STOPPED) {
            return false;
        }

        return this.canChooseDyeColorForSheep(villager, sheep);
    }

    private Optional<DyeColor> resolveDyeColor(@Nonnull BaseVillager villager, @Nonnull Sheep sheep) {
        if (isWololoTrigger(sheep)) {
            return Optional.of(WOLOLO_RESULT_COLOR);
        }

        if (this.selectedDyeColor == null || this.selectedDyeColor == sheep.getColor()) {
            return Optional.empty();
        }

        if (!villager.getSettlementsInventory()
                .containsOrBypassed(DyeItem.byColor(this.selectedDyeColor), GeneralConfig.bypassInventoryRequirements)) {
            return Optional.empty();
        }

        return Optional.of(this.selectedDyeColor);
    }

    private Optional<DyeColor> chooseBehaviorDyeColor(@Nonnull BaseVillager villager) {
        Map<DyeColor, Double> weightedChoices = Arrays.stream(DyeColor.values())
                .filter(color -> villager.getSettlementsInventory()
                        .containsOrBypassed(DyeItem.byColor(color), GeneralConfig.bypassInventoryRequirements))
                .filter(color -> this.hasEligibleSheepForColor(villager, color))
                .collect(Collectors.toMap(
                        color -> color,
                        DyeSheepBehavior::weightFor,
                        (left, right) -> left,
                        LinkedHashMap::new));

        if (weightedChoices.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(RandomUtil.weightedChoice(weightedChoices));
    }

    private boolean canChooseDyeColorForSheep(@Nonnull BaseVillager villager, @Nonnull Sheep sheep) {
        if (GeneralConfig.bypassInventoryRequirements) {
            return true;
        }
        // One inventory pass instead of a contains() scan per dye colour keeps this precondition hot path cheap.
        return villager.getSettlementsInventory()
                .anyMatch(stack -> stack.getItem() instanceof DyeItem dye && dye.getDyeColor() != sheep.getColor());
    }

    private boolean hasEligibleSheepForColor(@Nonnull BaseVillager villager, @Nonnull DyeColor color) {
        // Wololo sheep are excluded here: they recolor for free, so they never justify picking a run color.
        return this.getPerceivedEntities(villager)
                .ofType(Sheep.class, sheep -> this.isUndyedVillageSheep(sheep)
                        && !isWololoTrigger(sheep)
                        && sheep.getColor() != color)
                .findAny()
                .isPresent();
    }

    private PerceivedEntities getPerceivedEntities(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty());
    }

    private static boolean isVillageOwned(@Nonnull Sheep sheep) {
        return sheep.getTags().contains(EntityTag.VILLAGE_OWNED_ANIMAL.getTag());
    }

    private static boolean isWololoTrigger(@Nonnull Sheep sheep) {
        return sheep.getColor() == WOLOLO_TRIGGER_COLOR;
    }

    private static String describeColor(@Nonnull DyeColor color) {
        return color.getSerializedName().replace('_', ' ');
    }

    private static double weightFor(@Nonnull DyeColor color) {
        return switch (color) {
            case WHITE -> WHITE_WEIGHT;
            case BLACK -> BLACK_WEIGHT;
            case GRAY -> GRAY_WEIGHT;
            default -> OTHER_COLOR_WEIGHT;
        };
    }

}
