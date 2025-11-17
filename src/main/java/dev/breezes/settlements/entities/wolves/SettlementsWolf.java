package dev.breezes.settlements.entities.wolves;

import dev.breezes.settlements.bubbles.BubbleManager;
import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.villager.ISettlementsVillager;
import dev.breezes.settlements.entities.wolves.goals.WolfFollowOwnerGoal;
import dev.breezes.settlements.entities.wolves.goals.WolfSitWhenOrderedToGoal;
import dev.breezes.settlements.mixins.LevelMixin;
import dev.breezes.settlements.mixins.WolfMixin;
import dev.breezes.settlements.models.brain.DefaultBrain;
import dev.breezes.settlements.models.brain.IBrain;
import dev.breezes.settlements.models.navigation.INavigationManager;
import dev.breezes.settlements.models.navigation.VanillaBasicNavigationManager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@CustomLog
@Getter
public class SettlementsWolf extends Wolf implements ISettlementsBrainEntity {

    private final IBrain settlementsBrain;
    private final INavigationManager<SettlementsWolf> navigationManager;

    /**
     * TODO: we should use navigation manager or brain/goals/behavior to control wolf movement
     */
    @Deprecated
    private boolean stopFollowOwner;

    public SettlementsWolf(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);

        this.settlementsBrain = DefaultBrain.builder()
                .build(); // TODO: implement
        this.navigationManager = new VanillaBasicNavigationManager<>(this);

        // Initialize goals
        this.initGoals();

        // If not tamed by a villager, set as tamed by a random UUID to prevent players from taming it
        if (this.getOwnerUUID() == null) {
            this.setTame(true, true);
            this.setOwnerUUID(UUID.randomUUID());
            ((WolfMixin) this).invokeSetCollarColor(DyeColor.WHITE);
        }

        // Set step height to 1.5 (able to cross fences)
//        this.maxUpStep() = 1.5F;

//        this.stopFollowOwner = false;
//        this.lookLocked = false;
//        this.movementLocked = false;
    }

    private void initGoals() {
        // Replace default standby & follow goals
        this.goalSelector.removeAllGoals((goal -> goal instanceof SitWhenOrderedToGoal || goal instanceof FollowOwnerGoal));
        this.goalSelector.addGoal(2, new WolfSitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new WolfFollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));

        // Add target to all mobs that are hostile to villagers
        ISettlementsVillager.getEnemyClasses()
                .forEach(enemy -> this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, enemy, true)));
    }

    @Override
    @Nullable
    public BaseVillager getOwner() {
        if (this.getOwnerUUID() == null) {
            return null;
        }

        // Try to get the owner entity
        // TODO: THIS IS ASSUMING SERVER LEVEL
        return (BaseVillager) Optional.of(this.level())
                .filter(level -> level instanceof LevelMixin)
                .map(level -> (LevelMixin) level)
                .map(LevelMixin::invokeGetEntities)
                .map(entities -> entities.get(this.getOwnerUUID()))
                .filter(entity -> entity instanceof BaseVillager)
                .orElse(null);
    }


    @Override
    public IBrain getSettlementsBrain() {
        return null;
    }

    @Override
    public Entity getMinecraftEntity() {
        return this;
    }

    @Override
    public BubbleManager getBubbleManager() {
        return null;
    }

    @Override
    public int getNetworkingId() {
        return this.getId();
    }

}
