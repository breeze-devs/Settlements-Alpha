package dev.breezes.settlements.infrastructure.minecraft.entities.villager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleManager;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.inventory.GeneticInventoryProvider;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.application.ai.brain.CustomBehaviorPackages;
import dev.breezes.settlements.infrastructure.minecraft.ai.brain.CustomMemoryModuleType;
import dev.breezes.settlements.application.ai.brain.DefaultBrain;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.infrastructure.minecraft.navigation.VanillaMemoryNavigationManager;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

// TODO: make this class abstract
@CustomLog
@Getter
public class BaseVillager extends Villager implements ISettlementsVillager {

    private static final double DEFAULT_MOVEMENT_SPEED = 0.5D;
    private static final double DEFAULT_FOLLOW_RANGE = 48.0D;

    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES;
    private static final ImmutableList<SensorType<? extends Sensor<? super Villager>>> SENSOR_TYPES;

    private final GeneticsProfile genetics;
    private final IBrain settlementsBrain;
    private final INavigationManager<BaseVillager> navigationManager;
    private VillagerInventory settlementsInventory;

    private final BubbleManager bubbleManager;


    public BaseVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);

        this.genetics = new GeneticsProfile();

        // TODO: implement brain
        this.settlementsBrain = DefaultBrain.builder()
                .build();

        this.navigationManager = new VanillaMemoryNavigationManager<>(this);
        // VillagerPathNavigation navigation = new VillagerPathNavigation(this,
        // this.level());
        // navigation.setCanOpenDoors(true);
        // navigation.setCanFloat(true);
        // this.navigation = navigation;

        // Initialize custom inventory
        this.settlementsInventory = new GeneticInventoryProvider().provideDefault();
        this.bubbleManager = new BubbleManager();
    }

    public static AttributeSupplier createCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, DEFAULT_FOLLOW_RANGE)
                .build();
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbtTag) {
        super.addAdditionalSaveData(nbtTag);

        log.debug("Saving villager genetic profile");
        this.genetics.save(nbtTag);

        this.settlementsInventory.writeInventoryToTag(nbtTag, this.registryAccess());
    }

    @Override
    public void load(@Nonnull CompoundTag nbtTag) {
        super.load(nbtTag);
//        if (nbtTag.contains("SettlementsName")) {
//            this.settlementName = nbtTag.getString("SettlementsName");
//        }

        log.debug("Loading villager genetic profile");
        this.genetics.load(nbtTag);

        log.debug("Loading villager inventory");
        this.settlementsInventory = new GeneticInventoryProvider().provide(this.genetics);
        this.settlementsInventory.readInventoryFromTag(nbtTag, this.registryAccess());
    }

    @Override
    public void tick() {
        super.tick();

        // if (this.level().isClientSide()) {
        //     this.spinAnimator.tickAnimations(this.tickCount);
        // }
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

        // Register activities and behaviors
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

    @Override
    public BaseVillager getMinecraftEntity() {
        return this;
    }

    @Override
    public BubbleManager getBubbleManager() {
        return this.bubbleManager;
    }

    @Override
    public int getNetworkingId() {
        return this.getId();
    }

    @Override
    protected Brain.Provider<Villager> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        this.pickUp(itemEntity);
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        switch (this.getVillagerData().getProfession().name()) {
            case ("cleric") -> {
                if (shouldAddItem(itemStack,
                        Items.NETHER_WART,
                        Items.GLISTERING_MELON_SLICE,
                        Items.PUFFERFISH,
                        Items.MAGMA_CREAM,
                        Items.GHAST_TEAR))
                    return true;
            }
            case ("farmer") -> {
                if (shouldAddItem(itemStack,
                        Items.SUGAR_CANE,
                        Items.BONE_MEAL,
                        Items.WHEAT,
                        Items.WHEAT_SEEDS,
                        Items.BEETROOT_SEEDS,
                        Items.TORCHFLOWER_SEEDS,
                        Items.PITCHER_POD))
                    return true;
            }
            case ("shepherd") -> {
                if (shouldAddItem(itemStack,
                        Items.BLACK_WOOL,
                        Items.WHITE_WOOL))
                    return true;
            }
            default -> {
            }
        }
        // this definitely could be more compatible with other mods if we used mixins
        return shouldAddItem(itemStack, Items.BREAD, Items.POTATO, Items.CARROT, Items.BEETROOT);
    }

    private boolean shouldAddItem(ItemStack stackToAdd, Item... items) {
        VillagerInventory inventory = this.getSettlementsInventory();
        for (Item item : items) {
            if (stackToAdd.is(item) && inventory.countItem(item) < 64 && inventory.canAddItem(stackToAdd)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasItemInInventory(Item item) {
        return this.getSettlementsInventory().containsItem(item);
    }

    @Override
    public void pickUp(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (itemstack.isEmpty()) {
            return;
        }

        VillagerInventory inventory = this.getSettlementsInventory();
        Optional<ItemStack> leftover = inventory.addItem(itemstack);
        int leftoverCount = leftover.map(ItemStack::getCount).orElse(0);
        int takenCount = itemstack.getCount() - leftoverCount;

        if (takenCount <= 0) {
            return;
        }

        log.info("Picking up {}", itemEntity.getItem());
        this.onItemPickup(itemEntity);
        this.take(itemEntity, takenCount);
        itemstack.setCount(leftoverCount);
        itemEntity.setItem(itemstack);
    }

    static {
        MEMORY_TYPES = ImmutableList.of(
                MemoryModuleType.HOME,
                MemoryModuleType.JOB_SITE,
                MemoryModuleType.POTENTIAL_JOB_SITE,
                MemoryModuleType.MEETING_POINT,
                MemoryModuleType.NEAREST_LIVING_ENTITIES,
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                MemoryModuleType.VISIBLE_VILLAGER_BABIES,
                MemoryModuleType.NEAREST_PLAYERS,
                MemoryModuleType.NEAREST_VISIBLE_PLAYER,
                MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
                MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
                MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
                MemoryModuleType.WALK_TARGET,
                MemoryModuleType.LOOK_TARGET,
                MemoryModuleType.INTERACTION_TARGET,
                MemoryModuleType.BREED_TARGET,
                MemoryModuleType.PATH,
                MemoryModuleType.DOORS_TO_CLOSE,
                MemoryModuleType.NEAREST_BED,
                MemoryModuleType.HURT_BY,
                MemoryModuleType.HURT_BY_ENTITY,
                MemoryModuleType.NEAREST_HOSTILE,
                MemoryModuleType.SECONDARY_JOB_SITE,
                MemoryModuleType.HIDING_PLACE,
                MemoryModuleType.HEARD_BELL_TIME,
                MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                MemoryModuleType.LAST_SLEPT,
                MemoryModuleType.LAST_WOKEN,
                MemoryModuleType.LAST_WORKED_AT_POI,
                MemoryModuleType.GOLEM_DETECTED_RECENTLY,

                // Custom memory module types starts here
                CustomMemoryModuleType.FENCE_GATES_TO_CLOSE,
                MemoryTypeRegistry.NEAREST_HARVESTABLE_SUGARCANE.getModuleType(),
                MemoryTypeRegistry.INTERACT_TARGET.getModuleType());

        SENSOR_TYPES = ImmutableList.of(
                SensorType.NEAREST_LIVING_ENTITIES,
                SensorType.NEAREST_PLAYERS,
                SensorType.NEAREST_ITEMS,
                SensorType.NEAREST_BED,
                SensorType.HURT_BY,
                SensorType.VILLAGER_HOSTILES,
                SensorType.VILLAGER_BABIES,
                SensorType.SECONDARY_POIS,
                SensorType.GOLEM_DETECTED);
    }

}
