package dev.breezes.settlements.infrastructure.minecraft.entities.villager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.brain.DefaultBrain;
import dev.breezes.settlements.application.ai.brain.VanillaAmbientBehaviorPackages;
import dev.breezes.settlements.application.ai.brain.VanillaBehaviorPackages;
import dev.breezes.settlements.application.ai.planning.PlanRuntimeState;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleCommand;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleState;
import dev.breezes.settlements.bootstrap.registry.schedules.ScheduleRegistry;
import dev.breezes.settlements.bootstrap.registry.sensors.SensorTypeRegistry;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.entities.hunger.IVillagerHunger;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.inventory.GeneticInventoryProvider;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerBrainAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerDayPlanAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerGeneticsAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerHungerAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerInventoryAttachment;
import dev.breezes.settlements.infrastructure.minecraft.behavior.planning.PlanContextSwitcher;
import dev.breezes.settlements.infrastructure.minecraft.behavior.planning.PlanRunnerBehavior;
import dev.breezes.settlements.infrastructure.minecraft.navigation.VanillaMemoryNavigationManager;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleManager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// TODO: make this class abstract
@CustomLog
@Getter
public class BaseVillager extends Villager implements ISettlementsVillager, IVillagerHunger {

    private static final double DEFAULT_MOVEMENT_SPEED = 0.5D;
    private static final double DEFAULT_FOLLOW_RANGE = 48.0D;
    private static final int STARTING_BREAD_STACKS = 2;
    private static final int STARTING_BREAD_PER_STACK = 64;

    private final GeneticsProfile genetics;
    private final IBrain settlementsBrain;
    private final INavigationManager<BaseVillager> navigationManager;
    private final PlanRuntimeState planRuntimeState;
    @Nullable
    private VillagerInventory settlementsInventory;

    private final BubbleManager bubbleManager;
    private final VillagerBubbleState bubbleState;
    @Nullable
    private ITickable hungerDrainTimer;


    public BaseVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);

        this.genetics = new GeneticsProfile();

        // TODO: implement brain
        this.settlementsBrain = DefaultBrain.builder()
                .build();

        this.navigationManager = new VanillaMemoryNavigationManager<>(this);
        this.planRuntimeState = new PlanRuntimeState();
        // VillagerPathNavigation navigation = new VillagerPathNavigation(this,
        // this.level());
        // navigation.setCanOpenDoors(true);
        // navigation.setCanFloat(true);
        // this.navigation = navigation;

        this.bubbleManager = new BubbleManager();
        this.bubbleState = new VillagerBubbleState();
        this.hungerDrainTimer = null;
    }

    public static AttributeSupplier createCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, DEFAULT_FOLLOW_RANGE)
                .build();
    }

    private void addStartingFood(@Nonnull VillagerInventory inventory) {
        for (int i = 0; i < STARTING_BREAD_STACKS; i++) {
            inventory.addItem(new ItemStack(Items.BREAD, STARTING_BREAD_PER_STACK));
        }
    }

    public VillagerInventory getSettlementsInventory() {
        if (this.settlementsInventory == null) {
            throw new IllegalStateException("Inventory accessed before creation");
        }

        return this.settlementsInventory;
    }

    @Override
    public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor level,
                                        @Nonnull DifficultyInstance difficulty,
                                        @Nonnull MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData finalizedSpawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (this.settlementsInventory == null) {
            this.settlementsInventory = this.geneticInventoryProvider().provide(this.genetics);
            this.addStartingFood(this.settlementsInventory);
        }
        return finalizedSpawnData;
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbtTag) {
        super.addAdditionalSaveData(nbtTag);

        VillagerGeneticsAttachment.saveFrom(this, this.genetics);
        VillagerInventoryAttachment.saveFrom(this, this.getSettlementsInventory());
        VillagerBrainAttachment.saveFrom(this);
    }

    @Override
    public void load(@Nonnull CompoundTag nbtTag) {
        super.load(nbtTag);
        VillagerGeneticsAttachment.loadInto(this, this.genetics);
        VillagerBrainAttachment.loadInto(this);

        // Load inventory
        this.settlementsInventory = this.geneticInventoryProvider().provide(this.genetics);
        boolean loadedInventory = VillagerInventoryAttachment.loadInto(this, this.settlementsInventory);
        if (!loadedInventory) {
            // No persisted attachment state exists yet, so seed the default starting inventory.
            this.addStartingFood(this.settlementsInventory);
        }
    }

    private GeneticInventoryProvider geneticInventoryProvider() {
        // Minecraft controls entity construction via EntityType, so constructor injection is not possible here.
        return SettlementsDagger.serverOrThrow().geneticInventoryProvider();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            this.tickHunger();
            this.bubbleService().tick(this, this.level().getGameTime());
        }

        // if (this.level().isClientSide()) {
        //     this.spinAnimator.tickAnimations(this.tickCount);
        // }
    }

    @Override
    public void refreshBrain(@Nonnull ServerLevel level) {
        Brain<Villager> brain = this.getBrain();
        brain.stopAll(level, this);
        this.brain = brain.copyWithoutBehaviors();
        VillagerDayPlanAttachment.clearDayPlan(this);
        this.planRuntimeState.reset();
        this.registerBrainGoals(this.getBrain());
    }

    @Nullable
    public DayPlan getDayPlan() {
        return VillagerDayPlanAttachment.getDayPlan(this);
    }

    public void setDayPlan(@Nonnull DayPlan dayPlan) {
        VillagerDayPlanAttachment.setDayPlan(this, dayPlan);
    }

    /**
     * Core components copied from parent class
     */
    private void registerBrainGoals(Brain<Villager> brain) {
        // TODO: refactor speed, instead of hard coding 0.5F make it an attribute powered by genetics
        VillagerProfession profession = this.getVillagerData().getProfession();

        // Register core activity
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> corePackage = new ArrayList<>(
                VanillaBehaviorPackages.getCorePackage(profession, 0.5F));
        if (!this.isBaby()) {
            PlanRunnerBehavior planRunnerBehavior = SettlementsDagger.serverOrThrow()
                    .planRunnerBehaviorProvider()
                    .get();
            PlanContextSwitcher planContextSwitcher = new PlanContextSwitcher();
            corePackage.add(Pair.of(20, planRunnerBehavior));
            corePackage.add(Pair.of(98, planContextSwitcher));
        }
        brain.addActivity(Activity.CORE, ImmutableList.copyOf(corePackage));

        // Register work or play activities
        if (this.isBaby()) {
            this.registerBabyBrainGoals(brain, profession);
        } else {
            this.registerAdultAmbientBrainGoals(brain, profession);
        }

        // Register other activities
        brain.addActivity(Activity.PANIC, VanillaBehaviorPackages.getPanicPackage(profession, 0.5F));
        brain.addActivity(Activity.PRE_RAID, VanillaBehaviorPackages.getPreRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.RAID, VanillaBehaviorPackages.getRaidPackage(profession, 0.5F));
        brain.addActivity(Activity.HIDE, VanillaBehaviorPackages.getHidePackage(profession, 0.5F));

        // Core activities must be configured before the first setActiveActivityIfPossible call
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));

        // Set schedule
        if (this.isBaby()) {
            brain.setSchedule(Schedule.VILLAGER_BABY);
            brain.setDefaultActivity(Activity.IDLE);
            brain.setActiveActivityIfPossible(Activity.IDLE);
            brain.updateActivityFromSchedule(this.level().getDayTime(), this.level().getGameTime());
        } else {
            brain.setSchedule(ScheduleRegistry.SETTLEMENTS_SCHEDULE.get());
            brain.setDefaultActivity(Activity.IDLE);
            brain.setActiveActivityIfPossible(Activity.IDLE);
        }

        log.info("Registered plan-runner driven behavior orchestration");
    }

    private void registerBabyBrainGoals(Brain<Villager> brain,
                                        VillagerProfession profession) {
        // Babies stay on the vanilla schedule
        brain.addActivity(Activity.IDLE, VanillaBehaviorPackages.getIdlePackage(0.5F));
        brain.addActivity(Activity.PLAY, VanillaBehaviorPackages.getPlayPackage(0.5F));
        brain.addActivityWithConditions(Activity.MEET, VanillaBehaviorPackages.getMeetPackage(profession, 0.5F),
                Set.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        brain.addActivity(Activity.REST, VanillaBehaviorPackages.getRestPackage(profession, 0.5F));
    }

    private void registerAdultAmbientBrainGoals(Brain<Villager> brain,
                                                VillagerProfession profession) {
        float speed = 0.5F;
        brain.addActivityWithConditions(Activity.WORK, VanillaAmbientBehaviorPackages.getAmbientWorkPackage(profession, speed),
                Set.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        brain.addActivityWithConditions(Activity.MEET, VanillaAmbientBehaviorPackages.getAmbientMeetPackage(speed),
                Set.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        brain.addActivity(Activity.IDLE, VanillaAmbientBehaviorPackages.getAmbientIdlePackage(speed));
        brain.addActivity(Activity.REST, VanillaAmbientBehaviorPackages.getAmbientRestPackage(speed));
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

    @Override
    public float getHunger() {
        return VillagerHungerAttachment.getHunger(this);
    }

    @Override
    public void setHunger(float hunger) {
        VillagerHungerAttachment.setHunger(this, Math.clamp(hunger, 0.0f, 1.0f));
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
    public VillagerBubbleState getBubbleState() {
        return this.bubbleState;
    }

    @Override
    public void upsertBubble(@Nonnull BubbleChannel channel, @Nonnull String ownerKey, @Nonnull BubbleMessage message) {
        BubbleCommand.Upsert command = new BubbleCommand.Upsert(channel, ownerKey, message);
        this.bubbleService().applyCommand(this, command, this.level().getGameTime());
    }

    @Override
    public void removeBubbleByOwner(@Nonnull BubbleChannel channel, @Nonnull String ownerKey) {
        BubbleCommand.RemoveByOwner command = new BubbleCommand.RemoveByOwner(channel, ownerKey);
        this.bubbleService().applyCommand(this, command, this.level().getGameTime());
    }

    private VillagerBubbleService bubbleService() {
        // Minecraft controls entity construction via EntityType, so constructor injection is not possible here.
        return SettlementsDagger.serverOrThrow().villagerBubbleService();
    }

    @Override
    public int getNetworkingId() {
        return this.getId();
    }

    @Override
    protected Brain.Provider<Villager> brainProvider() {
        return Brain.provider(memoryTypes(), sensorTypes());
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
                        Items.WHEAT))
                    return true;
                if (shouldAddItem(itemStack, Tags.Items.SEEDS)) return true;
            }
            case ("shepherd") -> {
                if (shouldAddItem(itemStack,
                        ItemTags.WOOL))
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
            if (stackToAdd.is(item) && inventory.countItem(stackToAdd.getItem()) < 64 && inventory.canAddItem(stackToAdd)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldAddItem(ItemStack stackToAdd, TagKey<Item> tag) {
        VillagerInventory inventory = this.getSettlementsInventory();
        return stackToAdd.is(tag) && inventory.countItem(stackToAdd.getItem()) < 64 && inventory.canAddItem(stackToAdd);
    }

    public boolean hasItemInInventory(Item item) {
        return this.getSettlementsInventory().containsItem(item);
    }

    public boolean isTradeAvailable() {
        if (!this.isAlive() || this.isRemoved()) {
            return false;
        }

        // TODO: check that no behaviors are running

        // Trading partner scans need one canonical availability predicate so session ownership rules
        // stay consistent instead of drifting across multiple call sites.
        return SettlementsDagger.serverOrThrow()
                .tradeSessionRegistry()
                .getActiveSession(this.getUUID())
                .isEmpty();
    }

    private void tickHunger() {
        HungerConfig config = SettlementsDagger.component().hungerConfig();

        if (this.hungerDrainTimer == null) {
            this.hungerDrainTimer = Tickable.of(ClockTicks.seconds(config.tickIntervalSeconds()));
        }

        if (!this.hungerDrainTimer.tickCheckAndReset(1)) {
            return;
        }

        float baseDrain = config.drainPerInterval();
        double behaviorModifier = 1.0;
        IBehavior<BaseVillager> currentBehavior = this.planRuntimeState.getCurrentBehavior();
        if (currentBehavior != null && currentBehavior.getStatus() == BehaviorStatus.RUNNING
                && currentBehavior instanceof VillagerStateMachineBehavior villagerBehavior) {
            behaviorModifier = villagerBehavior.getHungerDrainModifier();
        }

        double sleepMultiplier = this.isSleeping() ? config.sleepingDrainMultiplier() : 1.0;
        this.setHunger((float) (this.getHunger() - baseDrain * behaviorModifier * sleepMultiplier));

        float hunger = this.getHunger();
        if (hunger < config.effectStartThreshold()) {
            int effectDuration = ClockTicks.seconds(config.tickIntervalSeconds() + 1).getTicksAsInt();
            int level = (int) Math.min(4, Math.ceil((config.effectStartThreshold() - hunger) / config.effectStartThreshold() * 4.0));

            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectDuration, level - 1, true, false));
            this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effectDuration, level - 1, true, false));
        }
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

    private static ImmutableList<MemoryModuleType<?>> memoryTypes() {
        return ImmutableList.of(
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
                MemoryTypeRegistry.FENCE_GATES_TO_CLOSE.getModuleType(),
                MemoryTypeRegistry.NEAREST_HARVESTABLE_SUGARCANE.getModuleType(),
                MemoryTypeRegistry.INTERACT_TARGET.getModuleType(),
                MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE.getModuleType(),
                MemoryTypeRegistry.OWNED_WOLVES.getModuleType());
    }

    private static ImmutableList<SensorType<? extends Sensor<? super Villager>>> sensorTypes() {
        return ImmutableList.of(
                SensorType.NEAREST_LIVING_ENTITIES,
                SensorType.NEAREST_PLAYERS,
                SensorType.NEAREST_ITEMS,
                SensorType.NEAREST_BED,
                SensorType.HURT_BY,
                SensorType.VILLAGER_HOSTILES,
                SensorType.VILLAGER_BABIES,
                SensorType.SECONDARY_POIS,
                SensorType.GOLEM_DETECTED,
                SensorTypeRegistry.OWNED_PETS_SENSOR.get()
        );
    }

}
