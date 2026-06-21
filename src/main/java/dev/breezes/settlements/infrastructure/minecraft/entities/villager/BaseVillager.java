package dev.breezes.settlements.infrastructure.minecraft.entities.villager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.teardown.ITeardownLedger;
import dev.breezes.settlements.application.ai.behavior.teardown.LedgerEntry;
import dev.breezes.settlements.application.ai.behavior.teardown.ProvidesTeardownLedger;
import dev.breezes.settlements.application.ai.behavior.teardown.TeardownObligation;
import dev.breezes.settlements.application.ai.brain.VanillaAmbientBehaviorPackages;
import dev.breezes.settlements.application.ai.brain.VanillaBehaviorPackages;
import dev.breezes.settlements.application.ai.brain.VillagerBrain;
import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.planning.PlanRuntimeState;
import dev.breezes.settlements.application.ai.socialcue.SocialCueRuntimeState;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleCommand;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleState;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.bootstrap.registry.schedules.ScheduleRegistry;
import dev.breezes.settlements.bootstrap.registry.sensors.SensorTypeRegistry;
import dev.breezes.settlements.di.ServerComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.observation.ObservationBuffer;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.entities.hunger.IVillagerHunger;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.ai.dialogue.ActivityOccasionMapper;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerBrainAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerCredibilityAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerDayPlanAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerGeneticsAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerHungerAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerInventoryAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerKnowledgeAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerTeardownLedger;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerTeardownLedgerAttachment;
import dev.breezes.settlements.infrastructure.minecraft.behavior.planning.PlanContextSwitcher;
import dev.breezes.settlements.infrastructure.minecraft.behavior.planning.PlanRunnerBehavior;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.genetics.VillagerGeneticAttributes;
import dev.breezes.settlements.infrastructure.minecraft.mixins.VillagerMixin;
import dev.breezes.settlements.infrastructure.minecraft.navigation.VanillaMemoryNavigationManager;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleManager;
import dev.breezes.settlements.shared.util.SyncedDataWrapper;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
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
public class BaseVillager extends Villager implements ISettlementsVillager, IVillagerHunger, ProvidesTeardownLedger {

    private static final double DEFAULT_MOVEMENT_SPEED = 0.5D;
    private static final double DEFAULT_FOLLOW_RANGE = 48.0D;
    private static final int STARTING_BREAD_STACKS = 2;
    private static final int STARTING_BREAD_PER_STACK = 64;
    private static final int MAX_DISCHARGE_ATTEMPTS = 10;
    private static final float BREED_HUNGER_THRESHOLD = 0.7F;
    private static final int BREED_FOOD_REQUIREMENT = 32;
    private static final ItemMatch FOODS_MATCH = new ItemMatch.TagRef(Tags.Items.FOODS);
    public static final float PANIC_DAMAGE_THRESHOLD = 0.1F;

    private static final ClockTicks RECONCILER_COOLDOWN_TICKS = ClockTicks.seconds(5);
    // Perception drains the WorldEventBus on a 1 Hz cadence instead of every tick. The bus is a
    // catch-up cursor, so batching only enlarges each delta — no events are missed as long as this
    // interval stays well under the bus TTL (EventLaneConfig.worldEventTtlTicks, default 100 ticks).
    private static final ClockTicks PERCEPTION_COOLDOWN_TICKS = ClockTicks.seconds(1);

    private static final SyncedDataWrapper<Byte> DATA_MOTION_ARCHETYPE = SyncedDataWrapper.<Byte>builder()
            .entityClass(BaseVillager.class)
            .serializer(EntityDataSerializers.BYTE)
            .defaultValue(AnimationArchetype.IDLE.toNetworkByte())
            .build();
    private static final SyncedDataWrapper<Byte> DATA_MOTION_GENERATION = SyncedDataWrapper.<Byte>builder()
            .entityClass(BaseVillager.class)
            .serializer(EntityDataSerializers.BYTE)
            .defaultValue((byte) 0)
            .build();
    private static final SyncedDataWrapper<Byte> DATA_LOCOMOTION_NAVIGATION_TYPE = SyncedDataWrapper.<Byte>builder()
            .entityClass(BaseVillager.class)
            .serializer(EntityDataSerializers.BYTE)
            .defaultValue(NavigationType.STROLL.toNetworkByte())
            .build();
    private static final SyncedDataWrapper<Boolean> DATA_BOBBER_DEPLOYED = SyncedDataWrapper.<Boolean>builder()
            .entityClass(BaseVillager.class)
            .serializer(EntityDataSerializers.BOOLEAN)
            .defaultValue(false)
            .build();
    private static final SyncedDataWrapper<Boolean> DATA_SOOTY = SyncedDataWrapper.<Boolean>builder()
            .entityClass(BaseVillager.class)
            .serializer(EntityDataSerializers.BOOLEAN)
            .defaultValue(false)
            .build();

    private final GeneticsProfile genetics;
    private final IBrain settlementsBrain;
    private final INavigationManager<BaseVillager> navigationManager;
    private final PlanRuntimeState planRuntimeState;
    private final SocialCueRuntimeState socialCueRuntimeState;
    private final VillagerKnowledgeStore knowledgeStore;
    @Nullable
    private VillagerInventory settlementsInventory;

    private final BubbleManager bubbleManager;
    private final VillagerBubbleState bubbleState;
    @Nullable
    private ITickable hungerDrainTimer;

    private VillagerTeardownLedger teardownLedger;
    private final ITickable reconcilerCooldown;
    private final ITickable perceptionCooldown;
    private long sootyExpiresAtGameTime;
    @Nullable
    private VillagerProfession cachedProfession;
    @Nullable
    private VillagerProfessionKey cachedProfessionKey;
    private float lastHurtAmount;


    public BaseVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);

        // TODO: I think we need to check dist side? i.e. most of this should run on server side
        this.genetics = new GeneticsProfile();

        this.settlementsBrain = new VillagerBrain(this);

        this.navigationManager = new VanillaMemoryNavigationManager<>(this);

        this.planRuntimeState = new PlanRuntimeState();
        EventLaneConfig eventLaneConfig = eventLaneConfigOrNull();
        int observationBufferCapacity = eventLaneConfig == null
                ? ObservationBuffer.DEFAULT_CAPACITY
                : eventLaneConfig.observationBufferCapacity();
        int knowledgeStoreMaxEntries = eventLaneConfig == null
                ? VillagerKnowledgeStore.MAX_ENTRIES
                : eventLaneConfig.knowledgeStoreMaxEntries();
        this.socialCueRuntimeState = new SocialCueRuntimeState(observationBufferCapacity);

        this.knowledgeStore = new VillagerKnowledgeStore(knowledgeStoreMaxEntries);

        // VillagerPathNavigation navigation = new VillagerPathNavigation(this,
        // this.level());
        // navigation.setCanOpenDoors(true);
        // navigation.setCanFloat(true);
        // this.navigation = navigation;

        this.bubbleManager = new BubbleManager();
        this.bubbleState = new VillagerBubbleState();
        this.hungerDrainTimer = null;

        // Start with an empty ledger; load() will replace it with the NBT-backed one.
        this.teardownLedger = new VillagerTeardownLedger(List.of());
        this.reconcilerCooldown = RECONCILER_COOLDOWN_TICKS.asTickable();
        this.perceptionCooldown = PERCEPTION_COOLDOWN_TICKS.asTickable();
        this.sootyExpiresAtGameTime = 0L;
    }

    @Nullable
    private static EventLaneConfig eventLaneConfigOrNull() {
        ServerComponent server = SettlementsDagger.serverOrNull();
        return server == null ? null : server.eventLaneConfig();
    }

    @Override
    public void lookAt(@Nonnull Location target) {
        this.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target.toBlockPos()));
    }

    public static AttributeSupplier createCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, DEFAULT_FOLLOW_RANGE)
                .build();
    }

    @Override
    protected void defineSynchedData(@Nonnull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        DATA_MOTION_ARCHETYPE.define(builder);
        DATA_MOTION_GENERATION.define(builder);
        DATA_LOCOMOTION_NAVIGATION_TYPE.define(builder);
        DATA_BOBBER_DEPLOYED.define(builder);
        DATA_SOOTY.define(builder);
    }

    @Override
    public boolean hurt(@Nonnull DamageSource source, float amount) {
        this.lastHurtAmount = amount;
        return super.hurt(source, amount);
    }

    /**
     * Sets the continuous animation state for this entity.
     * <p>
     * This method is <b>idempotent</b>. Setting the same archetype repeatedly
     * (e.g., every tick) will not interrupt or restart the current animation cycle.
     * Use this for continuous, looping states where the animation should persist
     * smoothly until a new state is applied.
     * <p>
     * <b>Examples:</b> {@code IDLE}, {@code REEL_OUT}, {@code EAT}
     *
     * @param archetype The continuous animation state to apply.
     * @see #triggerMotion(AnimationArchetype) for one-shot actions.
     */
    public void setMotion(@Nonnull AnimationArchetype archetype) {
        DATA_MOTION_ARCHETYPE.set(this.entityData, archetype.toNetworkByte());
    }

    /**
     * Triggers a discrete, one-shot animation, forcing it to play from the beginning.
     * <p>
     * Unlike {@link #setMotion(AnimationArchetype)}, calling this method will <i>always</i>
     * force the client to restart the animation from frame 0, even if the requested archetype
     * is the same as the current one.
     * <p>
     * <b>Examples:</b> {@code SWING_HEAVY}, {@code INTERACT}, {@code CAST}
     *
     * @param archetype The one-shot animation to trigger.
     */
    public void triggerMotion(@Nonnull AnimationArchetype archetype) {
        // Generation counter intentionally wraps from 127 to -128 on overflow
        DATA_MOTION_GENERATION.set(this.entityData, (byte) (DATA_MOTION_GENERATION.get(this.entityData) + 1));
        DATA_MOTION_ARCHETYPE.set(this.entityData, archetype.toNetworkByte());
    }

    public AnimationArchetype getMotion() {
        return AnimationArchetype.fromNetworkByte(DATA_MOTION_ARCHETYPE.get(this.entityData));
    }

    public byte getMotionGeneration() {
        return DATA_MOTION_GENERATION.get(this.entityData);
    }

    public void setLocomotionNavigationType(@Nonnull NavigationType navigationType) {
        DATA_LOCOMOTION_NAVIGATION_TYPE.set(this.entityData, navigationType.toNetworkByte());
    }

    public NavigationType getLocomotionNavigationType() {
        return NavigationType.fromNetworkByte(DATA_LOCOMOTION_NAVIGATION_TYPE.get(this.entityData));
    }

    public void setBobberDeployed(boolean deployed) {
        DATA_BOBBER_DEPLOYED.set(this.entityData, deployed);
    }

    public boolean isBobberDeployed() {
        return DATA_BOBBER_DEPLOYED.get(this.entityData);
    }

    public void setSooty(@Nonnull ClockTicks duration) {
        int ticks = Math.max(0, duration.getTicksAsInt());
        if (ticks <= 0) {
            this.sootyExpiresAtGameTime = 0L;
            DATA_SOOTY.set(this.entityData, false);
            return;
        }

        this.sootyExpiresAtGameTime = this.level().getGameTime() + ticks;
        DATA_SOOTY.set(this.entityData, true);
    }

    public boolean isSooty() {
        return DATA_SOOTY.get(this.entityData);
    }

    private void addStartingFood(@Nonnull VillagerInventory inventory) {
        for (int i = 0; i < STARTING_BREAD_STACKS; i++) {
            inventory.add(new ItemStack(Items.BREAD, STARTING_BREAD_PER_STACK));
        }
    }

    public VillagerInventory getSettlementsInventory() {
        if (this.settlementsInventory == null) {
            throw new IllegalStateException("Inventory accessed before creation");
        }

        return this.settlementsInventory;
    }

    public boolean hasSettlementsInventory() {
        return this.settlementsInventory != null;
    }

    public VillagerProfessionKey getProfession() {
        VillagerProfession currentProfession = this.getVillagerData().getProfession();
        if (this.cachedProfession != currentProfession || this.cachedProfessionKey == null) {
            // Profession is stable for long stretches but can change through vanilla data updates, so invalidate lazily.
            this.cachedProfession = currentProfession;
            this.cachedProfessionKey = VillagerProfessionKey.fromResourceLocation(BuiltInRegistries.VILLAGER_PROFESSION.getKey(currentProfession));
        }

        return this.cachedProfessionKey;
    }

    @Nullable
    @Override
    public BaseVillager getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob otherParent) {
        BaseVillager child = EntityRegistry.BASE_VILLAGER.get().create(level);
        if (child == null) {
            return null;
        }

        // Select child villager type
        VillagerType childType;
        double typeRoll = this.random.nextDouble();
        if (typeRoll < 0.50D) {
            childType = VillagerType.byBiome(level.getBiome(this.blockPosition()));
        } else if (typeRoll < 0.75D || !(otherParent instanceof Villager otherVillager)) {
            childType = this.getVillagerData().getType();
        } else {
            childType = otherVillager.getVillagerData().getType();
        }
        child.setVillagerData(child.getVillagerData().setType(childType));

        // Inherit genetics
        GeneticsProfile partnerGenetics = otherParent instanceof BaseVillager partner ? partner.getGenetics() : new GeneticsProfile();
        child.getGenetics().replaceWith(this.genetics.crossover(partnerGenetics, this.random));

        child.finalizeSpawn(level, level.getCurrentDifficultyAt(child.blockPosition()), MobSpawnType.BREEDING, null);
        return child;
    }

    @Override
    public boolean canBreed() {
        if (this.getAge() != 0 || this.isSleeping() || !this.hasSettlementsInventory()) {
            return false;
        }

        if (this.getHunger() <= BREED_HUNGER_THRESHOLD) {
            return false;
        }

        return this.getSettlementsInventory().countMatching(FOODS_MATCH) >= BREED_FOOD_REQUIREMENT;
    }

    /**
     * Returns true when this villager should not run or accept new social cues.
     * <p>
     * Future states (e.g. stunned, panicking, in-dialogue) should be added here rather than
     * in the arbiter so the arbiter stays free of lifecycle details.
     */
    public boolean isSociallyAvailable() {
        if (this.isSleeping() || this.isBaby()) {
            return false;
        }

        // Check active brain activity
        Optional<Activity> activity = this.getBrain().getActiveNonCoreActivity();
        if (activity.isEmpty()) {
            // If no activity is active, then we are socially available
            return true;
        }

        if (activity.get() == Activity.HIDE) {
            return false;
        }

        return true;
    }

    public Occasion getCurrentOccasion() {
        return ActivityOccasionMapper.map(this.getBrain().getActiveNonCoreActivity().orElse(Activity.IDLE));
    }

    @Override
    public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor level,
                                        @Nonnull DifficultyInstance difficulty,
                                        @Nonnull MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData finalizedSpawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        VillagerGeneticAttributes.apply(this);

        if (this.settlementsInventory == null) {
            this.settlementsInventory = new VillagerInventory();
            this.addStartingFood(this.settlementsInventory);
        }

        this.settlementsBrain.initialize();

        return finalizedSpawnData;
    }

    /**
     * Only ran on the server side
     */
    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbtTag) {
        super.addAdditionalSaveData(nbtTag);

        VillagerGeneticsAttachment.saveFrom(this, this.genetics);
        VillagerInventoryAttachment.saveFrom(this, this.getSettlementsInventory());
        VillagerBrainAttachment.saveFrom(this);
        VillagerTeardownLedgerAttachment.saveFrom(this, this.teardownLedger);
        VillagerKnowledgeAttachment.saveFrom(this, this.knowledgeStore);

        ServerComponent server = SettlementsDagger.serverOrThrow();
        VillagerCredibilityAttachment.saveFrom(this, server.reputationUtil());
    }

    @Override
    public void load(@Nonnull CompoundTag nbtTag) {
        super.load(nbtTag);
        VillagerGeneticsAttachment.loadInto(this, this.genetics);
        VillagerGeneticAttributes.apply(this);
        VillagerBrainAttachment.loadInto(this);
        this.teardownLedger = VillagerTeardownLedgerAttachment.loadInto(this);
        this.settlementsBrain.initialize();

        // Load inventory
        this.settlementsInventory = new VillagerInventory();
        boolean loadedInventory = VillagerInventoryAttachment.loadInto(this, this.settlementsInventory);
        if (!loadedInventory) {
            // No persisted attachment state exists yet, so seed the default starting inventory.
            this.addStartingFood(this.settlementsInventory);
        }

        // Restore episodic knowledge so investigation history and tip cooldowns survive restarts.
        VillagerKnowledgeAttachment.loadInto(this, this.knowledgeStore);

        // Restore credibility state into ReputationUtil so trust relationships survive log-in/out cycles.
        ServerComponent server = SettlementsDagger.serverOrThrow();
        VillagerCredibilityAttachment.loadInto(this, server.reputationUtil());
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            this.tickHunger();
            this.tickSooty();
            this.bubbleService().tick(this, this.level().getGameTime());
        }
    }

    private void tickSooty() {
        if (!DATA_SOOTY.get(this.entityData)) {
            return;
        }

        if (this.sootyExpiresAtGameTime > this.level().getGameTime()) {
            return;
        }

        this.sootyExpiresAtGameTime = 0L;
        DATA_SOOTY.set(this.entityData, false);
    }

    @Override
    protected void customServerAiStep() {
        ServerComponent server = SettlementsDagger.serverOrThrow();

        // Seed the event bus cursor before the brain tick so the override detector
        // (called inside the brain tick via PlanRunner.tickOverride) never drains
        // pre-load history on a freshly loaded villager. The cursor is transient and
        // is not persisted; the seed is a one-time skip-history operation.
        this.seedEventCursorOnFirstTick(server);

        super.customServerAiStep();

        this.settlementsBrain.tick(1);
        this.tickSocialCue(server);
        this.tickPerception(server);
        this.tickReconciler();
    }

    /**
     * On the first server AI step, seeds the WorldEventBus cursor to the current high-water mark
     * so this villager does not replay events that predate its load.
     */
    private void seedEventCursorOnFirstTick(@Nonnull ServerComponent server) {
        if (this.socialCueRuntimeState.getLastSeenSeq() != 0L) {
            return;
        }

        long currentBusSeq = server.worldEventBus().currentSeq();
        if (currentBusSeq > 0L) {
            this.socialCueRuntimeState.seedCursor(currentBusSeq);
        }
    }

    /**
     * Advances the SocialCue lane
     * <p>
     * Runs after the brain so the arbiter sees the freshest channel occupancy from the plan lane
     */
    private void tickSocialCue(@Nonnull ServerComponent server) {
        server.socialCueArbiter().tick(this, this.socialCueRuntimeState, this.level().getGameTime());
    }

    /**
     * Runs the perception pipeline
     * <p>
     * Runs after the brain tick (freshest sensor data) and the SocialCue tick.
     * Throttled to {@link #PERCEPTION_COOLDOWN_TICKS}; the pipeline is a catch-up cursor drain,
     * so a coarser cadence only batches the delta rather than dropping events.
     */
    private void tickPerception(@Nonnull ServerComponent server) {
        if (!this.perceptionCooldown.tickCheckAndReset(1)) {
            return;
        }

        long perceptionGameTime = this.getServer() != null
                ? this.getServer().overworld().getGameTime()
                : this.level().getGameTime();
        server.perceptionPipeline().tick(this, this.socialCueRuntimeState, perceptionGameTime);
    }

    /**
     * Processes crash-orphaned teardown obligations on a throttled cadence.
     * <p>
     * Each pass iterates only obligations whose target chunk is currently loaded,
     * deferring unloaded ones rather than counting them as failed attempts. This prevents
     * the circuit breaker from triggering on obligations that are simply waiting for a distant
     * chunk to load.
     */
    private void tickReconciler() {
        if (!this.reconcilerCooldown.tickCheckAndReset(1)) {
            return;
        }

        List<LedgerEntry> orphans = this.teardownLedger.pendingOrphans();
        if (orphans.isEmpty()) {
            return;
        }

        ServerLevel level = (ServerLevel) this.level();

        // Snapshot to a new list: resolveOrphan() modifies the underlying collection,
        // so we cannot iterate the live view and remove concurrently.
        for (LedgerEntry entry : new ArrayList<>(orphans)) {
            TeardownObligation obligation = entry.getObligation();

            if (!level.isLoaded(obligation.targetPos())) {
                // Target's chunk is not loaded — defer without counting an attempt.
                continue;
            }

            if (this.teardownLedger.hasLiveObligationAt(obligation.targetPos())) {
                // A live behavior run re-acquired this resource; it owns cleanup now.
                continue;
            }

            if (!obligation.stillValid(level)) {
                // Target gone, replaced, or no longer ours — nothing to do.
                this.teardownLedger.resolveOrphan(obligation);
                continue;
            }

            try {
                obligation.discharge(level);
            } catch (Exception e) {
                log.error("Failed to discharge crash-orphan obligation '{}': {}", obligation.describe(), e.getMessage());
            }

            if (!obligation.stillValid(level)) {
                // Discharge took effect.
                this.teardownLedger.resolveOrphan(obligation);
            } else {
                entry.incrementFailedAttempts();
                if (entry.getFailedAttempts() >= MAX_DISCHARGE_ATTEMPTS) {
                    log.error("Abandoning crash-orphan obligation '{}' after {} failed attempts — manual cleanup may be required",
                            obligation.describe(), MAX_DISCHARGE_ATTEMPTS);
                    this.teardownLedger.resolveOrphan(obligation);
                }
            }
        }
    }

    @Override
    public void remove(@Nonnull RemovalReason reason) {
        // Stop the running behavior before entity removal so TeardownScope obligations are discharged on death/unload.
        // The chain is:
        //   brain.stopAll → PlanRunnerBehavior.stop → planRunner.forceStop → behavior.stop → teardownAll
        // Must run before super.remove so the villager is still alive when discharge resolves entities.
        if (this.level() instanceof ServerLevel serverLevel) {
            this.getBrain().stopAll(serverLevel, this);
        }

        this.planRuntimeState.clearPendingGeneration();

        // Credibility is persisted on the entity, so the server-scope cache should only retain loaded observers
        if (!this.level().isClientSide() && shouldEvictCredibility(reason)) {
            ServerComponent server = SettlementsDagger.serverOrThrow();
            server.reputationUtil().removeObserver(this.getUUID());
        }

        super.remove(reason);
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
        VillagerProfession profession = this.getVillagerData().getProfession();

        // Register core activity
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> corePackage = new ArrayList<>(
                VanillaBehaviorPackages.getCorePackage(profession, this.navigationManager.speedFor(NavigationType.STROLL)));
        if (!this.isBaby()) {
            PlanRunnerBehavior planRunnerBehavior = SettlementsDagger.serverOrThrow()
                    .planRunnerBehaviorProvider()
                    .get();
            corePackage.add(Pair.of(20, planRunnerBehavior));

            PlanContextSwitcher planContextSwitcher = new PlanContextSwitcher();
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
        brain.addActivity(Activity.PANIC, VanillaBehaviorPackages.getPanicPackage(profession, this.navigationManager.speedFor(NavigationType.SPRINT)));
        brain.addActivity(Activity.PRE_RAID, VanillaBehaviorPackages.getPreRaidPackage(profession, this.navigationManager.speedFor(NavigationType.RUN)));
        brain.addActivity(Activity.RAID, VanillaBehaviorPackages.getRaidPackage(profession, this.navigationManager.speedFor(NavigationType.RUN)));
        brain.addActivity(Activity.HIDE, VanillaBehaviorPackages.getHidePackage(profession, this.navigationManager.speedFor(NavigationType.RUN)));

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
        brain.addActivity(Activity.IDLE, VanillaBehaviorPackages.getIdlePackage(this.navigationManager.speedFor(NavigationType.STROLL)));
        brain.addActivity(Activity.PLAY, VanillaBehaviorPackages.getPlayPackage(this.navigationManager.speedFor(NavigationType.RUN)));
        brain.addActivityWithConditions(Activity.MEET, VanillaBehaviorPackages.getMeetPackage(profession, this.navigationManager.speedFor(NavigationType.WALK)),
                Set.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        brain.addActivity(Activity.REST, VanillaBehaviorPackages.getRestPackage(profession, this.navigationManager.speedFor(NavigationType.STROLL)));
    }

    private void registerAdultAmbientBrainGoals(Brain<Villager> brain,
                                                VillagerProfession profession) {
        brain.addActivityWithConditions(Activity.WORK,
                VanillaAmbientBehaviorPackages.getAmbientWorkPackage(profession, this.navigationManager.speedFor(NavigationType.WALK)),
                Set.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        brain.addActivityWithConditions(Activity.MEET,
                VanillaAmbientBehaviorPackages.getAmbientMeetPackage(this.navigationManager.speedFor(NavigationType.WALK)),
                Set.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        brain.addActivity(Activity.IDLE, VanillaAmbientBehaviorPackages.getAmbientIdlePackage(this.navigationManager.speedFor(NavigationType.STROLL)));
        brain.addActivity(Activity.REST, VanillaAmbientBehaviorPackages.getAmbientRestPackage(this.navigationManager.speedFor(NavigationType.WALK)));
    }

    @Override
    public Optional<ItemStack> getHeldItem() {
        if (this.settlementsInventory != null && this.settlementsInventory.getMainHand().isPresent()) {
            return this.settlementsInventory.getMainHand();
        }

        // TODO: deprecate vanilla
        ItemStack vanillaMainHand = this.getItemInHand(InteractionHand.MAIN_HAND);
        return vanillaMainHand.isEmpty() ? Optional.empty() : Optional.of(vanillaMainHand);
    }

    @Override
    public void setHeldItem(@Nonnull ItemStack itemStack) {
        // TODO: deprecate vanilla
        this.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
        if (this.settlementsInventory != null) {
            this.settlementsInventory.setEquipped(EquipmentSlot.MAIN_HAND, itemStack);
        }
    }

    @Override
    public void clearHeldItem() {
        // TODO: deprecate vanilla
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (this.settlementsInventory != null) {
            this.settlementsInventory.setEquipped(EquipmentSlot.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    public void setOffhandItem(@Nonnull ItemStack itemStack) {
        this.setItemInHand(InteractionHand.OFF_HAND, itemStack);
        if (this.settlementsInventory != null) {
            this.settlementsInventory.setEquipped(EquipmentSlot.OFF_HAND, itemStack);
        }
    }

    public void clearOffhandItem() {
        this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (this.settlementsInventory != null) {
            this.settlementsInventory.setEquipped(EquipmentSlot.OFF_HAND, ItemStack.EMPTY);
        }
    }

    @Override
    @Nonnull
    public ITeardownLedger getTeardownLedger() {
        return this.teardownLedger;
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

    private static boolean shouldEvictCredibility(@Nonnull RemovalReason reason) {
        return reason == RemovalReason.KILLED
                || reason == RemovalReason.DISCARDED
                || reason.shouldSave();
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

    public void gainExperience(int amount) {
        this.setVillagerXp(this.getVillagerXp() + amount);
        VillagerMixin mixin = (VillagerMixin) this;
        if (mixin.invokeShouldIncreaseLevel()) {
            mixin.setIncreaseProfessionLevelOnUpdate(true);
            mixin.setUpdateMerchantTimer(ClockTicks.seconds(2).getTicksAsInt());
        }
    }

    @Override
    protected void pickUpItem(@Nonnull ItemEntity itemEntity) {
        // Vanilla pickup method, route it to the custom inventory
        this.pickUp(itemEntity);
    }

    @Override
    public boolean wantsToPickUp(@Nonnull ItemStack itemStack) {
        // Do not passively pick up outside of behaviors
        return false;
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

        int takenCount = itemstack.getCount();
        this.getSettlementsInventory().add(itemstack);

        log.info("Picking up {}", itemEntity.getItem());
        this.onItemPickup(itemEntity);
        this.take(itemEntity, takenCount);
        itemstack.setCount(0);
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
                MemoryTypeRegistry.INTERACT_TARGET.getModuleType(),
                MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE.getModuleType(),
                MemoryTypeRegistry.OWNED_WOLVES.getModuleType(),
                MemoryTypeRegistry.VILLAGE_CHESTS.getModuleType(),
                MemoryTypeRegistry.RIPE_PUMPKIN_SITES.getModuleType(),
                MemoryTypeRegistry.RIPE_MELON_SITES.getModuleType(),
                MemoryTypeRegistry.RIPE_SWEET_BERRY_BUSH_SITES.getModuleType(),
                MemoryTypeRegistry.RIPE_CROP_SITES.getModuleType(),
                MemoryTypeRegistry.NETHER_WART_FARM_SITES.getModuleType(),
                MemoryTypeRegistry.HARVESTABLE_SUGARCANE_SITES.getModuleType(),
                MemoryTypeRegistry.FULL_HIVE_SITES.getModuleType(),
                MemoryTypeRegistry.ORE_SITES.getModuleType(),
                MemoryTypeRegistry.WILLING_COURTSHIP_PARTNERS.getModuleType(),
                MemoryTypeRegistry.NEARBY_SENSED_ENTITIES.getModuleType(),
                MemoryTypeRegistry.GRAVEL_SITES.getModuleType(),
                MemoryTypeRegistry.SAND_SITES.getModuleType()
        );
    }

    private static ImmutableList<SensorType<? extends Sensor<? super Villager>>> sensorTypes() {
        return ImmutableList.of(
                SensorType.NEAREST_LIVING_ENTITIES,
                SensorType.NEAREST_PLAYERS,
                SensorType.NEAREST_ITEMS,
                SensorType.NEAREST_BED,
                SensorTypeRegistry.SETTLEMENTS_HURT_BY_SENSOR.get(),
                SensorType.VILLAGER_HOSTILES,
                SensorType.SECONDARY_POIS,
                SensorType.GOLEM_DETECTED,
                SensorTypeRegistry.SETTLEMENTS_VILLAGER_BABIES_SENSOR.get(),
                SensorTypeRegistry.OWNED_PETS_SENSOR.get(),
                SensorTypeRegistry.VILLAGE_CHESTS_SENSOR.get(),
                SensorTypeRegistry.WILLING_COURTSHIP_PARTNERS_SENSOR.get()
        );
    }

}
