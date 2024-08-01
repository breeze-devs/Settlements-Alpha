package dev.breezes.settlements.entities.villager;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import dev.breezes.settlements.entities.ISettlementsVillager;
import dev.breezes.settlements.entities.villager.animations.animator.ConditionAnimator;
import dev.breezes.settlements.entities.villager.animations.conditions.BooleanAnimationCondition;
import dev.breezes.settlements.entities.villager.animations.definitions.BaseVillagerAnimation;
import dev.breezes.settlements.models.behaviors.RepairIronGolemBehaviorAdapter;
import dev.breezes.settlements.models.brain.IBrain;
import dev.breezes.settlements.models.navigation.INavigationManager;
import dev.breezes.settlements.models.navigation.VanillaMemoryNavigationManager;
import dev.breezes.settlements.util.SyncedDataWrapper;
import lombok.Getter;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

// TODO: make this class abstract
@Getter
public class BaseVillager extends Villager implements ISettlementsVillager {

    private static final double DEFAULT_MOVEMENT_SPEED = 0.5D;
    private static final double DEFAULT_FOLLOW_RANGE = 48.0D;

    private static final Logger LOGGER = LogUtils.getLogger();

    private final IBrain settlementsBrain;
    private final INavigationManager<BaseVillager> navigationManager;
    private final ConditionAnimator wiggleAnimator;

    public BaseVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);

        this.settlementsBrain = null; // TODO: implement
        this.navigationManager = new VanillaMemoryNavigationManager<>(this);
        this.wiggleAnimator = new ConditionAnimator(this, BooleanAnimationCondition.of(SyncedData.IS_WIGGLING), List.of(BaseVillagerAnimation.NITWIT, BaseVillagerAnimation.ARM_CROSS), false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, DEFAULT_FOLLOW_RANGE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        SyncedData.defineAll(this.entityData);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.wiggleAnimator.tickAnimations(this.tickCount);
        }
    }

    @Override
    public void refreshBrain(@Nonnull ServerLevel level) {
        super.refreshBrain(level);

        // Add custom behaviors
        this.getBrain().addActivity(Activity.IDLE, ImmutableList.of(Pair.of(0, new RepairIronGolemBehaviorAdapter())));
    }

    @Override
    public boolean hurt(@Nonnull DamageSource pSource, float pAmount) {
        boolean wiggling = SyncedData.IS_WIGGLING.get(this.entityData);
        SyncedData.IS_WIGGLING.set(this.entityData, !wiggling);
        return super.hurt(pSource, pAmount);
    }

    /**
     * TODO: idk how this works, need to investigate
     */
    @Override
    protected void updateWalkAnimation(float partialTick) {
        float animationSpeed = 0;
        if (this.getPose() == Pose.STANDING) {
            animationSpeed = Math.min(partialTick * 6, 1);
        }

        this.walkAnimation.update(animationSpeed, 1F);
    }

    @Override
    public Optional<ItemStack> getHeldItem() {
        // TODO: implement
        return Optional.of(this.getItemInHand(InteractionHand.MAIN_HAND));
    }

    @Override
    public void setHeldItem(@Nonnull ItemStack itemStack) {
        // TODO: implement
        this.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
    }

    /**
     * Class for all the synced data for this entity, mostly used to play animations
     */
    public static class SyncedData {

        /**
         * Whether the arms are extended or crossed, with TRUE as extended
         */
        public static final SyncedDataWrapper<Boolean> ARMS_EXTENDED = SyncedDataWrapper.<Boolean>builder()
                .entityClass(BaseVillager.class)
                .serializer(EntityDataSerializers.BOOLEAN)
                .defaultValue(false)
                .build();

        /**
         * Whether the villager is idling or not. Idling
         */
        public static final SyncedDataWrapper<Boolean> PLAY_IDLE_ANIMATION = SyncedDataWrapper.<Boolean>builder()
                .entityClass(BaseVillager.class)
                .serializer(EntityDataSerializers.BOOLEAN)
                .defaultValue(false)
                .build();

        public static final SyncedDataWrapper<Boolean> IS_WIGGLING = SyncedDataWrapper.<Boolean>builder()
                .entityClass(BaseVillager.class)
                .serializer(EntityDataSerializers.BOOLEAN)
                .defaultValue(false)
                .build();

        /**
         * Registers all synced data defined here to the entity
         */
        public static void defineAll(SynchedEntityData entityData) {
            ARMS_EXTENDED.define(entityData);
            IS_WIGGLING.define(entityData);
            PLAY_IDLE_ANIMATION.define(entityData);
        }

    }

    public Logger getLogger() {
        return LOGGER;
    }

}
