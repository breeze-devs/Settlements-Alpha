package dev.breezes.settlements.entities.villager;

import com.mojang.logging.LogUtils;
import dev.breezes.settlements.entities.villager.animation.BaseVillagerAnimation;
import dev.breezes.settlements.entities.villager.animation.animator.ConditionAnimator;
import dev.breezes.settlements.entities.villager.animation.conditions.BooleanAnimationCondition;
import dev.breezes.settlements.util.SyncedDataWrapper;
import lombok.Getter;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

// TODO: make this class abstract
@Getter
public class BaseVillager extends Villager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ConditionAnimator wiggleAnimator;

    public BaseVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);

        this.wiggleAnimator = new ConditionAnimator(this, BooleanAnimationCondition.of(SyncedData.IS_WIGGLING), BaseVillagerAnimation.NITWIT.lengthInSeconds(), false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
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
    public boolean hurt(DamageSource pSource, float pAmount) {
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

    /**
     * Class for all the synced data for this entity
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
        }

    }

}
