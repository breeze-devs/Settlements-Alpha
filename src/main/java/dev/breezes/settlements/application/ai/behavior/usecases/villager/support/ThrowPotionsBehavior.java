package dev.breezes.settlements.application.ai.behavior.usecases.villager.support;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
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
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyFriendlyNeedsPotionCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.domain.world.location.Vector;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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
public class ThrowPotionsBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 4;

    private enum ThrowStage implements StageKey {
        THROW_POTION,
        END;
    }

    private final NearbyFriendlyNeedsPotionCondition<BaseVillager> nearbyFriendlyNeedsPotionCondition;

    @Nullable
    private LivingEntity targetToThrow;
    @Nullable
    private ItemStack potionToThrow;
    @Nullable
    private Holder<Potion> potionHolder;

    public ThrowPotionsBehavior(ThrowPotionsConfig config,
                                HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        // Preconditions to this behavior
        this.nearbyFriendlyNeedsPotionCondition = new NearbyFriendlyNeedsPotionCondition<>(config.scanRangeHorizontal(), config.scanRangeVertical(), config.minimumPlayerReputation());
        this.preconditions.add(this.nearbyFriendlyNeedsPotionCondition);

        // Initialize variables
        this.targetToThrow = null;
        this.potionToThrow = null;
        this.potionHolder = null;

        this.initializeStateMachine(this.createControlStep(), ThrowStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("ThrowPotionsBehavior")
                .initialStage(ThrowStage.THROW_POTION)
                .stageStepMap(Map.of(ThrowStage.THROW_POTION, this.createThrowStep()))
                .nextStage(ThrowStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createThrowStep() {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 3))
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
                    if (!GeneralConfig.bypassInventoryRequirements && this.potionHolder != null) {
                        this.consumeMatchingSplashPotion(villager, this.potionHolder);
                    }
                    villager.clearHeldItem();
                    this.rewardExperience(villager);

                    return StepResult.complete();
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {

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
            this.requestStop("No target found to throw potion at");
            return;
        }

        Holder<Potion> potionNeeded = this.nearbyFriendlyNeedsPotionCondition.getFriendlyNeedsPotionMap().get(this.targetToThrow).getPotion();
        Optional<ItemStack> foundPotion = findMatchingSplashPotionInInventory(villager, potionNeeded);
        if (GeneralConfig.bypassInventoryRequirements) {
            this.potionToThrow = foundPotion.orElseGet(() -> PotionContents.createItemStack(Items.SPLASH_POTION, potionNeeded));
        } else {
            if (foundPotion.isEmpty()) {
                this.requestStop("No matching splash potion in inventory");
                return;
            }
            this.potionToThrow = foundPotion.get().copyWithCount(1);
        }
        this.potionHolder = potionNeeded;

        context.setState(BehaviorStateType.TARGET, TargetState.of(List.of(Targetable.fromEntity(this.targetToThrow))));
        villager.setHeldItem(this.potionToThrow);
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.targetToThrow != null && this.targetToThrow.isAlive();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        this.targetToThrow = null;
        this.potionToThrow = null;
        this.potionHolder = null;
    }

    private static Optional<ItemStack> findMatchingSplashPotionInInventory(@Nonnull BaseVillager villager,
                                                                           @Nonnull Holder<Potion> needed) {
        return villager.getSettlementsInventory().findFirst(stack -> isMatchingSplashPotion(stack, needed));
    }

    private static void consumeMatchingSplashPotion(@Nonnull BaseVillager villager,
                                                    @Nonnull Holder<Potion> needed) {
        villager.getSettlementsInventory()
                .findFirst(stack -> isMatchingSplashPotion(stack, needed))
                .ifPresent(stack -> villager.getSettlementsInventory().consume(stack, 1));
    }

    private static boolean isMatchingSplashPotion(@Nonnull ItemStack stack,
                                                  @Nonnull Holder<Potion> needed) {
        if (!stack.is(Items.SPLASH_POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().filter(h -> h.value() == needed.value()).isPresent();
    }

}
