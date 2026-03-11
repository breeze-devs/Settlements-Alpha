package dev.breezes.settlements.application.ai.behavior.usecases.villager.support;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyFriendlyNeedsPotionCondition;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.domain.world.location.Vector;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
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

@CustomLog
public class ThrowPotionsBehavior extends StateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 4;

    private enum ThrowStage implements StageKey {
        THROW_POTION,
        END;
    }

    private final NearbyFriendlyNeedsPotionCondition<BaseVillager> nearbyFriendlyNeedsPotionCondition;
    @Getter
    private final BehaviorDescriptor behaviorDescriptor;

    @Nullable
    private LivingEntity targetToThrow;
    @Nullable
    private ItemStack potionToThrow;

    public ThrowPotionsBehavior(ThrowPotionsConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())));
        this.behaviorDescriptor = BehaviorDescriptor.builder()
                .displayNameKey("ui.settlements.behavior.behavior.throw_potions")
                .iconItemId(ResourceLocation.withDefaultNamespace("splash_potion"))
                .displaySuffix(null)
                .build();

        // Preconditions to this behavior
        this.nearbyFriendlyNeedsPotionCondition = new NearbyFriendlyNeedsPotionCondition<>(config.scanRangeHorizontal(), config.scanRangeVertical(), config.minimumPlayerReputation());
        this.preconditions.add(this.nearbyFriendlyNeedsPotionCondition);

        // Initialize variables
        this.targetToThrow = null;
        this.potionToThrow = null;

        this.initializeStateMachine(this.createControlStep(), ThrowStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("ThrowPotionsBehavior")
                .initialStage(ThrowStage.THROW_POTION)
                .stageStepMap(Map.of(ThrowStage.THROW_POTION, this.createThrowStep()))
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

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext context) {

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

        context.setState(BehaviorStateType.TARGET, TargetState.of(List.of(Targetable.fromEntity(this.targetToThrow))));
        villager.setHeldItem(this.potionToThrow);
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        return this.targetToThrow != null && this.targetToThrow.isAlive();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        this.targetToThrow = null;
        this.potionToThrow = null;
    }

}
