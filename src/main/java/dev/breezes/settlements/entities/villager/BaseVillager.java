package dev.breezes.settlements.entities.villager;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.entities.ISettlementsVillager;
import dev.breezes.settlements.entities.villager.animations.animator.OneShotAnimator;
import dev.breezes.settlements.entities.villager.animations.definitions.BaseVillagerAnimation;
import dev.breezes.settlements.models.brain.CustomBehaviorPackages;
import dev.breezes.settlements.models.brain.IBrain;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.Expertise;
import dev.breezes.settlements.models.navigation.INavigationManager;
import dev.breezes.settlements.models.navigation.VanillaMemoryNavigationManager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        Brain<Villager> brain = this.getBrain();
        brain.stopAll(level, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    /**
     * Core components copied from parent class
     */
    private void registerBrainGoals(Brain<Villager> brain) {
        VillagerProfession profession = this.getVillagerData().getProfession();

        // Register activities & behaviors
        brain.addActivity(Activity.CORE, CustomBehaviorPackages.getCorePackage(profession, 0.5F).behaviors());
        brain.addActivity(Activity.IDLE, CustomBehaviorPackages.getIdlePackage(profession, 0.5F).behaviors());

        if (this.isBaby()) {
            // If baby, register PLAY activities
            brain.addActivity(Activity.PLAY, CustomBehaviorPackages.getPlayPackage(0.5F));
        } else {
            // Otherwise, register WORK activities if job site is present
            brain.addActivityWithConditions(Activity.WORK, CustomBehaviorPackages.getWorkPackage(profession, 0.5F).behaviors(),
                    ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        }

        // Register meet activities if meeting point is present
        brain.addActivityWithConditions(Activity.MEET, CustomBehaviorPackages.getMeetPackage(profession, 0.5F).behaviors(),
                Set.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));

        // Register other activities
        brain.addActivity(Activity.REST, CustomBehaviorPackages.getRestPackage(profession, 0.5F));
        brain.addActivity(Activity.PANIC, CustomBehaviorPackages.getPanicPackage(profession, 0.5F));
        brain.addActivity(Activity.PRE_RAID, CustomBehaviorPackages.getPreRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.RAID, CustomBehaviorPackages.getRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.HIDE, CustomBehaviorPackages.getHidePackage(profession, 0.5F));

        // Set schedule
        if (this.isBaby()) {
            brain.setSchedule(Schedule.VILLAGER_BABY);
        } else {
            brain.setSchedule(Schedule.VILLAGER_DEFAULT);
        }

        // Configure activities
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(this.level().getDayTime(), this.level().getGameTime());
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

    public Expertise getExpertise() {
        return Expertise.fromLevel(this.getVillagerData().getLevel());
    }

    public Location getLocation(boolean useEyeHeight) {
        return Location.fromEntity(this, useEyeHeight);
    }

    public Location getLocation() {
        return Location.fromEntity(this, true);
    }

    @Override
    public BaseVillager getMinecraftEntity() {
        return this;
    }

}
