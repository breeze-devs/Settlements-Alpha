package dev.breezes.settlements.entities.villager;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.entities.ISettlementsVillager;
import dev.breezes.settlements.entities.villager.animations.animator.OneShotAnimator;
import dev.breezes.settlements.entities.villager.animations.definitions.BaseVillagerAnimation;
import dev.breezes.settlements.models.behaviors.DefaultBehaviorAdapter;
import dev.breezes.settlements.models.behaviors.RepairIronGolemBehavior;
import dev.breezes.settlements.models.brain.IBrain;
import dev.breezes.settlements.models.navigation.INavigationManager;
import dev.breezes.settlements.models.navigation.VanillaMemoryNavigationManager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

// TODO: make this class abstract
@CustomLog
@Getter
public class BaseVillager extends Villager implements ISettlementsVillager {

    private static final double DEFAULT_MOVEMENT_SPEED = 0.5D;
    private static final double DEFAULT_FOLLOW_RANGE = 48.0D;

    private final IBrain settlementsBrain;
    private final INavigationManager<BaseVillager> navigationManager;

    public final OneShotAnimator spinAnimator;

    public BaseVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);

        this.settlementsBrain = null; // TODO: implement
        this.navigationManager = new VanillaMemoryNavigationManager<>(this);

        this.spinAnimator = new OneShotAnimator("SpinAnimator", this, List.of(BaseVillagerAnimation.SPIN));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, DEFAULT_FOLLOW_RANGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.spinAnimator.tickAnimations(this.tickCount);
        }
    }

    @Override
    public void refreshBrain(@Nonnull ServerLevel level) {
        super.refreshBrain(level);

        // Add custom behaviors
        this.getBrain().addActivity(Activity.IDLE, ImmutableList.of(Pair.of(0, new DefaultBehaviorAdapter(new RepairIronGolemBehavior()))));
    }

    @Override
    public Optional<ItemStack> getHeldItem() {
        return Optional.of(this.getItemInHand(InteractionHand.MAIN_HAND));
    }

    @Override
    public void setHeldItem(@Nonnull ItemStack itemStack) {
        this.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
    }

    public void clearHeldItem() {
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

}
