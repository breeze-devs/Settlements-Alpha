package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.StageKey;
import dev.breezes.settlements.models.behaviors.steps.StepResult;
import dev.breezes.settlements.models.behaviors.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.StayCloseStep;
import dev.breezes.settlements.models.conditions.NearbyFriendlyNeedsPotionCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.location.Vector;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class ThrowPotionsBehavior extends BaseVillagerStagedBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 4;

    private enum ThrowStage implements StageKey {
        THROW_POTION,
        END;
    }

    private final ThrowPotionsConfig config;
    private final StagedStep controlStep;
    private final NearbyFriendlyNeedsPotionCondition<BaseVillager> nearbyFriendlyNeedsPotionCondition;

    @Nullable
    private LivingEntity targetToThrow;
    @Nullable
    private ItemStack potionToThrow;

    @Nullable
    private BehaviorContext context;

    public ThrowPotionsBehavior(ThrowPotionsConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())));
        this.config = config;

        // Preconditions to this behavior
        this.nearbyFriendlyNeedsPotionCondition = new NearbyFriendlyNeedsPotionCondition<>(config.scanRangeHorizontal(), config.scanRangeVertical(), config.minimumPlayerReputation());
        this.preconditions.add(this.nearbyFriendlyNeedsPotionCondition);

        // Initialize variables
        this.targetToThrow = null;
        this.potionToThrow = null;
        this.context = null;

        this.controlStep = StagedStep.builder()
                .name("ThrowPotionsBehavior")
                .initialStage(ThrowStage.THROW_POTION)
                .stageStepMap(Map.of(
                        ThrowStage.THROW_POTION, this.createThrowStep()
                ))
                .nextStage(ThrowStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep createThrowStep() {
        return StayCloseStep.builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep(0.5f, 3))
                .actionStep(ctx -> {
                    if (this.targetToThrow == null || this.potionToThrow == null) {
                        return StepResult.fail("Target or potion is null");
                    }

                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();

                    Location location = Location.fromEntity(villager, true);
                    SoundRegistry.THROW_POTION.playGlobally(location, SoundSource.NEUTRAL);

                    ThrownPotion potionEntity = new ThrownPotion(villager.level(), villager);
                    potionEntity.setItem(this.potionToThrow);
                    potionEntity.setXRot(potionEntity.getXRot() + 20.0F);

                    Vector direction = villager.getLocation().getDirectionTo(Location.fromEntity(this.targetToThrow, true));
                    potionEntity.shoot(direction.getX(), direction.getY(), direction.getZ(), 1, 4.0F);

                    villager.level().addFreshEntity(potionEntity);
                    villager.clearHeldItem();

                    return StepResult.complete();
                })
                .build();
    }

    public void doStart(@Nonnull Level world, @Nonnull BaseVillager villager) {
        this.context = new BehaviorContext(villager);

        double currentHpPercentage = Double.MAX_VALUE;
        for (Entity entity : this.nearbyFriendlyNeedsPotionCondition.getFriendlyNeedsPotionMap().keySet()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            double hp = livingEntity.getHealth() / livingEntity.getMaxHealth();
            if (hp < currentHpPercentage) {
                this.targetToThrow = livingEntity;
                currentHpPercentage = hp;
            }
        }

        if (this.targetToThrow == null) {
            log.behaviorStatus("No target found to throw potion at");
            this.requestStop();
            return;
        }

        Holder<Potion> potionNeeded = this.nearbyFriendlyNeedsPotionCondition.getFriendlyNeedsPotionMap().get(this.targetToThrow).getPotion();
        this.potionToThrow = PotionContents.createItemStack(Items.SPLASH_POTION, potionNeeded);

        this.context.setState(BehaviorStateType.TARGET, TargetState.of(List.of(Targetable.fromEntity(this.targetToThrow))));
        villager.setHeldItem(this.potionToThrow);
    }

    @Override
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.context == null) {
            throw new StopBehaviorException("Behavior context is null");
        }

        Optional<LivingEntity> target = this.context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(Targetable::getAsEntity)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast);
        if (target.isEmpty()) {
            throw new StopBehaviorException("No potion target found");
        }
        this.targetToThrow = target.get();

        StepResult result = this.controlStep.tick(this.context);
        this.handleStepResult(result, ThrowStage.END, "ThrowPotionsBehavior");
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        this.targetToThrow = null;
        this.potionToThrow = null;
        this.context = null;
        this.controlStep.reset();
    }

}
