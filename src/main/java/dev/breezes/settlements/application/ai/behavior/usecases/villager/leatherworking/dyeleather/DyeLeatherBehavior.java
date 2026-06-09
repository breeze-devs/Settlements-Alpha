package dev.breezes.settlements.application.ai.behavior.usecases.villager.leatherworking.dyeleather;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.teardown.DiscardEntityObligation;
import dev.breezes.settlements.application.ai.behavior.teardown.TemporaryArtifactHandle;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.look.LookState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.SequencedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.mixins.ArmorStandMixin;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class DyeLeatherBehavior extends VillagerStateMachineBehavior {

    // The villager works from the open block beside the stand, so the action must trigger from one block out
    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    // Horizontal radius centered on the cauldron to search for a free stand spot
    private static final int STAND_SEARCH_RADIUS = 4;

    private static final ClockTicks PLACE_STAND_DURATION = ClockTicks.seconds(1);
    private static final ClockTicks EQUIP_PIECE_DURATION = ClockTicks.seconds(0.6);
    private static final ClockTicks DYE_DURATION = ClockTicks.seconds(1);
    private static final ClockTicks DRY_DURATION = ClockTicks.seconds(5);
    private static final ClockTicks UNEQUIP_PIECE_DURATION = ClockTicks.seconds(0.5);
    private static final ClockTicks RETRIEVE_STAND_DURATION = ClockTicks.seconds(1);

    private static final int DRY_PARTICLE_INTERVAL_TICKS = 5;

    private enum DyeStage implements StageKey {
        DYE_LEATHER,
        END;
    }

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;

    @Nullable
    private PhysicalBlock cauldron;
    @Nullable
    private BlockPos cauldronPos;
    @Nullable
    private Location standLocation;
    @Nullable
    private DyeColor chosenColor;
    @Nullable
    private ArmorStand armorStand;
    @Nullable
    private TemporaryArtifactHandle standHandle;

    public DyeLeatherBehavior(DyeLeatherConfig config, HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(),
                hungerConfig, config.experienceReward());

        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.CAULDRON));
        this.preconditions.add(this.jobSiteBlockExistsCondition);

        this.cauldron = null;
        this.cauldronPos = null;
        this.standLocation = null;
        this.chosenColor = null;
        this.armorStand = null;
        this.standHandle = null;

        this.initializeStateMachine(this.createControlStep(), DyeStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("DyeLeatherArmorBehavior")
                .initialStage(DyeStage.DYE_LEATHER)
                .stageStepMap(Map.of(
                        DyeStage.DYE_LEATHER, this.createDyeStep()
                ))
                .nextStage(DyeStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.jobSiteBlockExistsCondition.getJobSiteBlock().isEmpty()) {
            this.requestStop("No cauldron block found at job site");
            return;
        }

        this.cauldron = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();
        this.cauldronPos = this.cauldron.getLocation(false).toBlockPos();

        Optional<StandPlacement> placement = this.resolveStandPlacement(world);
        if (placement.isEmpty()) {
            this.requestStop("No open spot near the cauldron to place an armor stand");
            return;
        }

        StandPlacement standPlacement = placement.get();
        this.standLocation = Location.of(standPlacement.standPos(), world).center(true);
        this.chosenColor = RandomUtil.choice(DyeColor.values());

        // Walk to the open block in front of the stand rather than the cauldron
        Location approachCenter = Location.of(standPlacement.approachPos(), world).center(true);
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromLocation(approachCenter)));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.cauldronPos == null) {
            return false;
        }

        BlockState current = world.getBlockState(this.cauldronPos);
        return current.is(Blocks.CAULDRON) || current.is(Blocks.WATER_CAULDRON);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        // The armor stand is cleaned up by TeardownScope.teardownAll() via the tracked DiscardEntityObligation
        this.cauldron = null;
        this.cauldronPos = null;
        this.standLocation = null;
        this.chosenColor = null;
        this.armorStand = null;
        this.standHandle = null;
    }

    private BehaviorStep<BaseVillager> createDyeStep() {
        TimeBasedStep<BaseVillager> placeStand = TimeBasedStep.<BaseVillager>builder()
                .withTickable(PLACE_STAND_DURATION.asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.ARMOR_STAND.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.standLocation == null || this.cauldronPos == null) {
                        return StepResult.complete();
                    }

                    BaseVillager villager = ctx.getInitiator();
                    // Face the stand toward the villager, snapped to 45° increments
                    double dx = villager.getX() - this.standLocation.getX();
                    double dz = villager.getZ() - this.standLocation.getZ();
                    float faceYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                    float snappedYaw = Mth.floor((Mth.wrapDegrees(faceYaw) + 22.5F) / 45.0F) * 45.0F;
                    this.standLocation.setYaw(snappedYaw);

                    ArmorStand stand = new ArmorStand(ctx.getLevel(), this.standLocation.getX(), this.standLocation.getY(), this.standLocation.getZ());

                    // Set armor stand as non-interactable
                    stand.setNoGravity(true);
                    stand.setInvulnerable(true);
                    //noinspection DataFlowIssue
                    ((ArmorStandMixin) stand).invokeSetMarker(true);

                    // Placement sound matches vanilla ArmorStandItem behavior
                    this.standLocation.playSound(SoundEvents.ARMOR_STAND_PLACE, 0.75f, 0.8f, SoundSource.BLOCKS);

                    this.standLocation.spawnEntity(stand);
                    this.armorStand = stand;
                    this.standHandle = ctx.getTeardownScope().track(new DiscardEntityObligation(stand.getUUID(), this.standLocation.toBlockPos()));

                    ctx.getInitiator().clearHeldItem();

                    // Lock gaze onto the stand the villager actually works on (chest height)
                    ctx.setState(BehaviorStateType.LOOK_TARGET, LookState.ofLocation(this.standLocation.add(0, 1.2, 0, true)));
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> equipFeet = this.equipBeat(EquipmentSlot.FEET, Items.LEATHER_BOOTS);
        TimeBasedStep<BaseVillager> equipLegs = this.equipBeat(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS);
        TimeBasedStep<BaseVillager> equipChest = this.equipBeat(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
        TimeBasedStep<BaseVillager> equipHead = this.equipBeat(EquipmentSlot.HEAD, Items.LEATHER_HELMET);

        TimeBasedStep<BaseVillager> dyeFeet = this.dyeBeat(EquipmentSlot.FEET, Items.LEATHER_BOOTS);
        TimeBasedStep<BaseVillager> dyeLegs = this.dyeBeat(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS);
        TimeBasedStep<BaseVillager> dyeChest = this.dyeBeat(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
        TimeBasedStep<BaseVillager> dyeHead = this.dyeBeat(EquipmentSlot.HEAD, Items.LEATHER_HELMET);

        TimeBasedStep<BaseVillager> dry = TimeBasedStep.<BaseVillager>builder()
                .withTickable(DRY_DURATION.asTickable())
                .addPeriodicStep(DRY_PARTICLE_INTERVAL_TICKS, ctx -> {
                    if (this.standLocation == null || this.chosenColor == null) {
                        return StepResult.noOp();
                    }

                    // Emit particles at chest height so they appear to waft off the drying armor
                    Location chestHeight = this.standLocation.add(0, 1.2, 0, true);
                    int particleColor = this.chosenColor.getTextureDiffuseColor();
                    ColorParticleOption dyeParticle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, particleColor);
                    chestHeight.displayParticles(dyeParticle, 4, 0.2, 0.2, 0.2, 0.0);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.standLocation == null || this.chosenColor == null) {
                        return StepResult.noOp();
                    }
                    Location chestHeight = this.standLocation.add(0, 1, 0, true);
                    chestHeight.displayParticles(ParticleTypes.WAX_OFF, 15, 0.4, 1, 0.4, 0.1);
                    chestHeight.playSound(SoundEvents.WET_SPONGE_DRIES, 0.4f, 1.3f, SoundSource.NEUTRAL);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> unequipHead = this.unequipBeat(EquipmentSlot.HEAD, Items.LEATHER_HELMET);
        TimeBasedStep<BaseVillager> unequipChest = this.unequipBeat(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
        TimeBasedStep<BaseVillager> unequipLegs = this.unequipBeat(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS);
        TimeBasedStep<BaseVillager> unequipFeet = this.unequipBeat(EquipmentSlot.FEET, Items.LEATHER_BOOTS);

        TimeBasedStep<BaseVillager> retrieve = TimeBasedStep.<BaseVillager>builder()
                .withTickable(RETRIEVE_STAND_DURATION.asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.ARMOR_STAND.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.standHandle != null) {
                        this.standHandle.dispose(ctx.getLevel());
                        this.standHandle = null;
                    }
                    this.armorStand = null;

                    ctx.getInitiator().clearHeldItem();
                    this.rewardExperience(ctx.getInitiator().getMinecraftEntity());
                    return StepResult.complete();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(new SequencedStep<>("DyeLeatherBehavior.sequence",
                        List.of(placeStand, equipFeet, equipLegs, equipChest, equipHead,
                                dyeFeet, dyeLegs, dyeChest, dyeHead,
                                dry,
                                unequipHead, unequipChest, unequipLegs, unequipFeet,
                                retrieve)))
                .build();
    }

    /**
     * Returns a beat that has the villager hold the raw leather piece and place it undyed onto the stand.
     */
    private TimeBasedStep<BaseVillager> equipBeat(EquipmentSlot slot, Item leatherItem) {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(EQUIP_PIECE_DURATION.asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(new ItemStack(leatherItem));
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.armorStand != null) {
                        this.armorStand.setItemSlot(slot, new ItemStack(leatherItem));
                    }
                    ctx.getInitiator().clearHeldItem();
                    if (this.standLocation != null) {
                        // ARMOR_EQUIP_LEATHER matches the action of slotting a leather piece onto the stand;
                        // .value() unwraps the Holder<SoundEvent> to the plain SoundEvent required by playSound.
                        this.standLocation.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.5f, 1.0f, SoundSource.PLAYERS);
                    }
                    return StepResult.noOp();
                })
                .build();
    }

    /**
     * Returns a beat that has the villager hold the dye and tint a single equipped piece that color.
     */
    private TimeBasedStep<BaseVillager> dyeBeat(EquipmentSlot slot, Item leatherItem) {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(DYE_DURATION.asTickable())
                .onStart(ctx -> {
                    if (this.chosenColor != null) {
                        ctx.getInitiator().setHeldItem(new ItemStack(DyeItem.byColor(this.chosenColor)));
                    }
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    if (this.armorStand == null || this.chosenColor == null) {
                        return StepResult.noOp();
                    }
                    // getTextureDiffuseColor() is opaque ARGB; mask the alpha for the tint (vanilla stores RGB only), keep it for the particle.
                    int armorRgb = this.chosenColor.getTextureDiffuseColor() & 0x00FFFFFF;
                    this.applyDyeToSlot(slot, leatherItem, armorRgb);
                    if (this.standLocation != null) {
                        int particleColor = this.chosenColor.getTextureDiffuseColor();
                        ColorParticleOption dyeParticle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, particleColor);
                        this.standLocation.add(0, 1.2, 0, true).displayParticles(dyeParticle, 5, 0.25, 0.3, 0.25, 0.0);
                        this.standLocation.playSound(SoundEvents.ITEM_PICKUP, 0.4f, 0.9f, SoundSource.BLOCKS);
                    }
                    return StepResult.noOp();
                })
                .build();
    }

    /**
     * Returns a beat that has the villager mime picking a dyed piece off the stand and clearing the slot.
     */
    private TimeBasedStep<BaseVillager> unequipBeat(EquipmentSlot slot, Item leatherItem) {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(UNEQUIP_PIECE_DURATION.asTickable())
                .onStart(ctx -> {
                    // Show the dyed piece being "retrieved" in hand
                    if (this.chosenColor != null) {
                        int armorRgb = this.chosenColor.getTextureDiffuseColor() & 0x00FFFFFF;
                        ItemStack dyedStack = new ItemStack(leatherItem);
                        dyedStack.set(DataComponents.DYED_COLOR, new DyedItemColor(armorRgb, true));
                        ctx.getInitiator().setHeldItem(dyedStack);
                    } else {
                        ctx.getInitiator().setHeldItem(new ItemStack(leatherItem));
                    }
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.armorStand != null) {
                        this.armorStand.setItemSlot(slot, ItemStack.EMPTY);
                    }
                    ctx.getInitiator().clearHeldItem();
                    if (this.standLocation != null) {
                        this.standLocation.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.5f, 1.0f, SoundSource.PLAYERS);
                    }
                    return StepResult.noOp();
                })
                .build();
    }

    /**
     * Applies the dye tint to a single armor slot on the stand.
     */
    private void applyDyeToSlot(EquipmentSlot slot, Item leatherItem, int armorRgb) {
        if (this.armorStand == null) {
            return;
        }
        ItemStack stack = new ItemStack(leatherItem);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(armorRgb, true));
        this.armorStand.setItemSlot(slot, stack);
    }

    /**
     * Scans a horizontal area centered on the cauldron for a free spot to place the armor stand,
     * paired with an adjacent open block for the villager to stand in. Both the stand spot and its
     * approach must be open standing columns so the stand has headroom and the villager works in front
     * of it. Returns a random qualifying placement, or empty if the cauldron is boxed in.
     */
    private Optional<StandPlacement> resolveStandPlacement(@Nonnull Level world) {
        if (this.cauldronPos == null) {
            return Optional.empty();
        }

        List<StandPlacement> placements = new ArrayList<>();
        for (int dx = -STAND_SEARCH_RADIUS; dx <= STAND_SEARCH_RADIUS; dx++) {
            for (int dz = -STAND_SEARCH_RADIUS; dz <= STAND_SEARCH_RADIUS; dz++) {
                BlockPos standPos = this.cauldronPos.offset(dx, 0, dz);
                if (!isOpenStandingColumn(world, standPos)) {
                    continue;
                }
                this.findApproach(world, standPos)
                        .ifPresent(approachPos -> placements.add(new StandPlacement(standPos, approachPos)));
            }
        }

        if (placements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(RandomUtil.choice(placements));
    }

    /**
     * Returns the first horizontally adjacent open standing column the villager can occupy to work on a
     * stand placed at {@code standPos}.
     */
    private Optional<BlockPos> findApproach(@Nonnull Level world, @Nonnull BlockPos standPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos approachPos = standPos.relative(direction);
            if (isOpenStandingColumn(world, approachPos)) {
                return Optional.of(approachPos);
            }
        }
        return Optional.empty();
    }

    /**
     * A column is open for a ~2-block-tall occupant (the stand or the villager) when it has two stacked
     * air blocks resting on solid ground. The cauldron's own position fails the air check and is excluded.
     */
    private static boolean isOpenStandingColumn(@Nonnull Level world, @Nonnull BlockPos pos) {
        return world.getBlockState(pos).isAir()
                && world.getBlockState(pos.above()).isAir()
                && !world.getBlockState(pos.below()).isAir();
    }

    /**
     * A resolved stand placement: where the armor stand is spawned and the adjacent open block the
     * villager stands in to work on it.
     */
    private record StandPlacement(BlockPos standPos, BlockPos approachPos) {
    }

}
